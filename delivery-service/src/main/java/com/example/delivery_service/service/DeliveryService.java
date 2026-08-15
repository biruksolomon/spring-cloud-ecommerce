package com.example.delivery_service.service;

import com.example.delivery_service.domain.Address;
import com.example.delivery_service.domain.Delivery;
import com.example.delivery_service.domain.DeliveryStatus;
import com.example.delivery_service.dto.AddressDto;
import com.example.delivery_service.dto.DeliveryStatusUpdateRequest;
import com.example.delivery_service.event.DeliveryStatusEvent;
import com.example.delivery_service.event.OrderCreatedEvent;
import com.example.delivery_service.exception.DeliveryNotFoundException;
import com.example.delivery_service.exception.InvalidDeliveryStatusTransitionException;
import com.example.delivery_service.exception.UnauthorizedDeliveryAccessException;
import com.example.delivery_service.publisher.KafkaDeliveryStatusPublisher;
import com.example.delivery_service.repository.DeliveryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class DeliveryService {

    // The shipment state machine. Only moves listed here are legal via
    // the status-update API; anything else (e.g. skipping straight from
    // PROCESSING to DELIVERED, or moving out of a terminal status) is
    // rejected. FAILED can retry back into OUT_FOR_DELIVERY, or be
    // written off as RETURNED.
    private static final Map<DeliveryStatus, Set<DeliveryStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(DeliveryStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(DeliveryStatus.PENDING, EnumSet.of(DeliveryStatus.PROCESSING, DeliveryStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(DeliveryStatus.PROCESSING, EnumSet.of(DeliveryStatus.DISPATCHED, DeliveryStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(DeliveryStatus.DISPATCHED, EnumSet.of(DeliveryStatus.IN_TRANSIT, DeliveryStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(DeliveryStatus.IN_TRANSIT, EnumSet.of(DeliveryStatus.OUT_FOR_DELIVERY, DeliveryStatus.FAILED));
        ALLOWED_TRANSITIONS.put(DeliveryStatus.OUT_FOR_DELIVERY, EnumSet.of(DeliveryStatus.DELIVERED, DeliveryStatus.FAILED));
        ALLOWED_TRANSITIONS.put(DeliveryStatus.FAILED, EnumSet.of(DeliveryStatus.OUT_FOR_DELIVERY, DeliveryStatus.RETURNED));
        ALLOWED_TRANSITIONS.put(DeliveryStatus.DELIVERED, EnumSet.noneOf(DeliveryStatus.class));
        ALLOWED_TRANSITIONS.put(DeliveryStatus.CANCELLED, EnumSet.noneOf(DeliveryStatus.class));
        ALLOWED_TRANSITIONS.put(DeliveryStatus.RETURNED, EnumSet.noneOf(DeliveryStatus.class));
    }

    private final DeliveryRepository deliveryRepository;
    private final KafkaDeliveryStatusPublisher publisher;

    public DeliveryService(DeliveryRepository deliveryRepository, KafkaDeliveryStatusPublisher publisher) {
        this.deliveryRepository = deliveryRepository;
        this.publisher = publisher;
    }

    // Opens a new shipment for an order as soon as it's placed (status
    // PENDING - the shipment exists but isn't actionable until payment is
    // confirmed). Idempotent: a redelivered/duplicate OrderCreatedEvent
    // for an order that already has a shipment is a no-op rather than a
    // second row or a thrown exception, since Kafka only guarantees
    // at-least-once delivery.
    public void openDelivery(OrderCreatedEvent event) {
        if (deliveryRepository.existsByOrderId(event.getOrderId())) {
            log.info("Delivery already exists for orderId {}, ignoring duplicate OrderCreatedEvent", event.getOrderId());
            return;
        }

        Delivery delivery = new Delivery();
        delivery.setOrderId(event.getOrderId());
        delivery.setUserId(event.getUserId());
        delivery.setProductId(event.getProductId());
        delivery.setQuantity(event.getQuantity());
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setOrigin(toAddress(event.getOriginAddress()));
        delivery.setDestination(toAddress(event.getDeliveryAddress()));
        delivery.addTrackingEvent(DeliveryStatus.PENDING, null, "Shipment created, awaiting payment confirmation");

        deliveryRepository.save(delivery);
        log.info("Opened delivery for orderId {}", event.getOrderId());
    }

    // Reacts to the order's payment outcome: CONFIRMED moves the shipment
    // into PROCESSING so logistics can start packing it; CANCELLED cancels
    // the shipment outright. Any other order status is irrelevant here.
    public void applyOrderStatus(Long orderId, String orderStatus) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId).orElse(null);
        if (delivery == null) {
            log.warn("Received order status event for orderId {} with no matching delivery yet", orderId);
            return;
        }

        if ("CONFIRMED".equals(orderStatus) && delivery.getStatus() == DeliveryStatus.PENDING) {
            transition(delivery, DeliveryStatus.PROCESSING, null, null, "Payment confirmed, ready for dispatch", null, null);
        } else if ("CANCELLED".equals(orderStatus) && canStillCancel(delivery.getStatus())) {
            transition(delivery, DeliveryStatus.CANCELLED, null, null, "Order was cancelled", null, null);
        }
    }

    private boolean canStillCancel(DeliveryStatus status) {
        return status == DeliveryStatus.PENDING || status == DeliveryStatus.PROCESSING;
    }

    // Admin/logistics-driven status update, e.g. handing a package to a
    // carrier or marking it delivered.
    public Delivery updateStatus(Long deliveryId, DeliveryStatusUpdateRequest request) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

        if (request.getCarrier() != null) {
            delivery.setCarrier(request.getCarrier());
        }
        if (request.getTrackingNumber() != null) {
            delivery.setTrackingNumber(request.getTrackingNumber());
        }
        if (request.getEstimatedDeliveryDate() != null) {
            delivery.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
        }

        transition(delivery, request.getStatus(), request.getCarrier(), request.getTrackingNumber(),
                request.getNote(), request.getLocation(), request.getEstimatedDeliveryDate());

        return delivery;
    }

    private void transition(Delivery delivery, DeliveryStatus newStatus, String carrier, String trackingNumber,
                             String note, String location, Long estimatedDeliveryDate) {

        DeliveryStatus current = delivery.getStatus();
        if (current == newStatus) {
            // Re-applying the same status (e.g. a retried request) is a
            // harmless no-op rather than an error.
            return;
        }

        Set<DeliveryStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(DeliveryStatus.class));
        if (!allowed.contains(newStatus)) {
            throw new InvalidDeliveryStatusTransitionException(current, newStatus);
        }

        delivery.setStatus(newStatus);
        delivery.setUpdatedAt(System.currentTimeMillis());
        if (newStatus == DeliveryStatus.DELIVERED) {
            delivery.setActualDeliveryDate(System.currentTimeMillis());
        }
        delivery.addTrackingEvent(newStatus, location, note);

        deliveryRepository.save(delivery);

        publisher.publish(new DeliveryStatusEvent(
                delivery.getDeliveryId(),
                delivery.getOrderId(),
                delivery.getUserId(),
                delivery.getStatus().name(),
                location,
                delivery.getUpdatedAt()
        ));

        log.info("Delivery {} (order {}) moved {} -> {}", delivery.getDeliveryId(), delivery.getOrderId(), current, newStatus);
    }

    public Delivery getById(Long deliveryId, Long callerUserId, boolean isAdmin) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));
        enforceOwnership(delivery, callerUserId, isAdmin);
        return delivery;
    }

    public Delivery getByOrderId(Long orderId, Long callerUserId, boolean isAdmin) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> DeliveryNotFoundException.forOrder(orderId));
        enforceOwnership(delivery, callerUserId, isAdmin);
        return delivery;
    }

    private void enforceOwnership(Delivery delivery, Long callerUserId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (callerUserId == null || !callerUserId.equals(delivery.getUserId())) {
            throw new UnauthorizedDeliveryAccessException(delivery.getDeliveryId());
        }
    }

    public Page<Delivery> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return deliveryRepository.findAll(pageable);
    }

    private Address toAddress(AddressDto dto) {
        Address address = new Address();
        if (dto != null) {
            address.setRecipientName(dto.getRecipientName());
            address.setPhone(dto.getPhone());
            address.setStreet(dto.getStreet());
            address.setCity(dto.getCity());
            address.setState(dto.getState());
            address.setPostalCode(dto.getPostalCode());
            address.setCountry(dto.getCountry());
        }
        return address;
    }
}

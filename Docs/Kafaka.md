# Kafka in This Project — Mini Guide

A short reference for how async messaging works here (`order-service` → `notification-service`), and the concepts to remember for future services.

## 1. Why Kafka here?

`order-service` needs to tell `notification-service` "an order was created" — but it shouldn't wait around for notifications to be sent, and it shouldn't care if `notification-service` is temporarily down. Kafka decouples the two: `order-service` drops an event in a topic and moves on; `notification-service` reads it whenever it's ready.

Analogy: it's a mailbox, not a phone call. `order-service` posts a letter and gets on with its day; it doesn't need `notification-service` to pick up.

## 2. The moving parts

| Concept | What it is | Where it lives here |
|---|---|---|
| **Topic** | Named channel messages are published to | `order-events` |
| **Producer** | Publishes messages to a topic | `order-service`'s `KafkaOrderPublisher` |
| **Consumer** | Reads messages from a topic | `notification-service`'s `OrderConsumer` |
| **Consumer group** | A named set of consumers sharing the work of one topic | `notification-group` |
| **Serializer / Deserializer** | Converts your Java object ↔ bytes on the wire | `MyJsonSerializer` / `MyJsonDeserializer` |

## 3. The flow, step by step

1. A client calls `order-service` to place an order.
2. `OrderService.createOrder(...)` saves the order, then calls `KafkaOrderPublisher`.
3. `KafkaOrderPublisher` sends an `OrderCreatedEvent` to the `order-events` topic.
4. `notification-service`'s `OrderConsumer` (in consumer group `notification-group`) picks it up and calls `NotificationService.saveNotification(...)`.
5. Only once that save succeeds does the consumer call `acknowledgment.acknowledge()` — telling Kafka "I'm done, don't give me this message again."

## 4. Reliability settings we rely on (and why)

**Producer side** (`KafkaProducerConfig`):
- `acks=all` — wait for all in-sync replicas to confirm before considering the send successful. Without this, a message can be "sent" from your code's point of view but never actually persisted.
- `retries=5` — automatically retry on transient broker/network errors instead of failing the whole order creation.
- `enable.idempotence=true` — guarantees a retry can never accidentally create a duplicate message. Retries + no idempotence = risk of double-processing an order.

**Consumer side** (`KafkaConsumerConfig`, `OrderConsumer`):
- `AckMode.MANUAL` — the container won't auto-commit an offset just because a message was *read*; it waits for your code to explicitly say "processed successfully."
- We only call `acknowledgment.acknowledge()` **after** `saveNotification` succeeds. If it throws, the message is not acknowledged and the error handler takes over.

## 5. What happens when processing fails

`DefaultErrorHandler` + `FixedBackOff` (in `KafkaConsumerConfig`):
- Retries the failed message twice, 1 second apart.
- If it still fails, `DeadLetterPublishingRecoverer` republishes it to `order-events.DLT` (dead-letter topic) instead of retrying forever or silently dropping it.
- `DeadLetterConsumer` listens on `order-events.DLT` and logs it for manual follow-up (today just a log line — a natural place to add alerting, an incidents table, or a replay endpoint later).

Analogy: three delivery attempts, then the letter goes in the "returned mail" bin at the post office instead of blocking the mail truck or vanishing.

## 6. Local setup

Kafka + Zookeeper (or KRaft) must be running before either service starts — see the project's `docker-compose.yml`. If `order-service` throws connection errors on order creation, check the broker is up on `localhost:9092` first.

## 7. Checklist for adding a new Kafka producer/consumer pair later

- [ ] Define the event DTO (keep it simple, versionable, no entity leakage).
- [ ] Producer: set `acks=all`, `retries`, `enable.idempotence=true`.
- [ ] Consumer: `AckMode.MANUAL` + explicit `acknowledgment.acknowledge()` after success, never before.
- [ ] Attach a `DefaultErrorHandler` with backoff + a dead-letter recoverer — don't ship a consumer without one.
- [ ] Decide a consumer group name deliberately — it determines how work is shared/scaled.
- [ ] Log failures with enough context (order/entity ID) to actually act on a DLT message later.

## 8. Common pitfalls this project already hit

- Forgetting to call `acknowledge()` under `AckMode.MANUAL` — messages appear "consumed" in logs but Kafka never commits the offset.
- A dead-letter *consumer* class existing in code with no `@KafkaListener` on it — dead code, not a safety net.
- No `acks`/`idempotence` config on the producer — silent message loss or duplication under retries.

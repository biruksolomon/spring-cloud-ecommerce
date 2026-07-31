package com.example.payment_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootTest
@EmbeddedKafka(
		partitions = 1,
		topics = { "order-events", "order-events.DLT", "payment-events" },
		brokerProperties = { "auto.create.topics.enable=true" }
)
class PaymentServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
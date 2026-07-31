package com.example.notification_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;


@SpringBootTest
@EmbeddedKafka(
		partitions = 1,
		topics = { "order-events", "order-events.DLT" },
		brokerProperties = { "auto.create.topics.enable=true" }
)
class NotificationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
package com.example.notification_service.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE = "notification.queue";

/*    public static final String EXCHANGE = "order.exchange";

    public static final String ROUTING_KEY = "order.created";*/

    public static final String DLQ = "notification.dlq";

    public static final String DLX = "notification.dlx";

    public static final String DL_ROUTING_KEY = "notification.failed";

    @Bean
    Queue notificationQueue() {

        return QueueBuilder
                .durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DL_ROUTING_KEY)
                .build();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);

        return factory;
    }


    /*
    * Create Dead Letter Queue
    * */
    @Bean
    public Queue deadLetterQueue(){
        return QueueBuilder
                .durable(DLQ)
                .build();
    }

    /*
    * Create Dead Letter Exchange
    * */

    @Bean
    public DirectExchange deadLetterExchange(){
        return new DirectExchange(DLX);
    }

    /*
    * Bind DLQ
    * */
    @Bean
    public Binding deadLetterBinding(){
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DL_ROUTING_KEY);
    }

}

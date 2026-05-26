package com.nexis.execution_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMqConfig {

    public static final String QUEUE_NAME = "execution.queue";
    public static final String EXCHANGE_NAME = "execution.exchange";
    public static final String ROUTING_KEY = "execution.routing.key";

    public static final String RESULT_EXCHANGE = "result.exchange";
    public static final String RESULT_ROUTING_KEY = "result.routing.key";

    // Notice we do NOT create a Queue here!
    // The Execution Service is just the Publisher now.
    // The WebSocket Service will create the Queue when we build it later.
    @Bean
    public Queue executionQueue() {
        // The 'true' parameter means durable.
        // If the RabbitMQ server restarts, the queue survives.
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public DirectExchange executionExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public DirectExchange resultExchange() { return new DirectExchange(RESULT_EXCHANGE);}

    @Bean
    public Binding binding(Queue executionQueue, DirectExchange executionExchange) {
        return BindingBuilder
                .bind(executionQueue)
                .to(executionExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}

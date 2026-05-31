package com.nexis.websocket_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

    public static final String HASH_EXCHANGE = "nexis.exchange.hash";

    public static final String CHAT_QUEUE = "nexis.chat.queue";
    public static final String CHAT_ROUTING_KEY = "nexis.chat.routing.key";
    public static final String TOPIC_EXCHANGE = "nexis.exchange";


    public static final String CODE_SHARD_0 = "nexis.code.queue.shard0";
    public static final String CODE_SHARD_1 = "nexis.code.queue.shard1";
    public static final String CODE_SHARD_2 = "nexis.code.queue.shard2";

    @Bean
    public Queue chatQueue(){
        return new Queue(CHAT_QUEUE, true);
    }

    /*
    Instead of one massive line, you split the store into 3 separate lanes (Shard 0, Shard 1, and Shard 2).
    Each lane is a completely independent, smaller queue.
    */

    @Bean
    public Queue codeShard0() {
        return QueueBuilder.durable(CODE_SHARD_0)
                .withArgument("x-single-active-consumer", true)
                .build();
    }
    @Bean
    public Queue codeShard1() {
        return QueueBuilder.durable(CODE_SHARD_1)
                .withArgument("x-single-active-consumer", true)
                .build();
    }
    @Bean
    public Queue codeShard2() {
        return QueueBuilder.durable(CODE_SHARD_2)
                .withArgument("x-single-active-consumer", true)
                .build();
    }


    @Bean
    public CustomExchange consistentHashExchange() {
        return new CustomExchange(HASH_EXCHANGE, "x-consistent-hash");
    }

    @Bean
    public TopicExchange topicExchange(){
        return new TopicExchange(TOPIC_EXCHANGE);
    }

    @Bean
    public Binding chatBinding(Queue chatQueue, TopicExchange topicExchange){
        return BindingBuilder.bind(chatQueue).to(topicExchange).with(CHAT_ROUTING_KEY);
    }

    /* * Bind Shards to Hash Exchange.
     * The routing key ("1") specifies the hashing weight bucket.
     */
    @Bean
    public Binding bindShard0(Queue codeShard0, CustomExchange consistentHashExchange) {
        return BindingBuilder.bind(codeShard0).to(consistentHashExchange).with("1").noargs();
    }

    @Bean
    public Binding bindShard1(Queue codeShard1, CustomExchange consistentHashExchange) {
        return BindingBuilder.bind(codeShard1).to(consistentHashExchange).with("1").noargs();
    }

    @Bean
    public Binding bindShard2(Queue codeShard2, CustomExchange consistentHashExchange) {
        return BindingBuilder.bind(codeShard2).to(consistentHashExchange).with("1").noargs();
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitConvertor(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(rabbitConvertor());
        return template;
    }
}
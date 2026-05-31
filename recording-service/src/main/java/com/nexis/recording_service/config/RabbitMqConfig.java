package com.nexis.recording_service.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        // Enforces that all objects sent/received via RabbitTemplate are converted to/from JSON bytes
        return new JacksonJsonMessageConverter();
    }

    public static final String CODE_SHARD_0 = "nexis.code.queue.shard0";
    public static final String CODE_SHARD_1 = "nexis.code.queue.shard1";
    public static final String CODE_SHARD_2 = "nexis.code.queue.shard2";

    public static final String CHAT_QUEUE = "nexis.chat.queue";
}
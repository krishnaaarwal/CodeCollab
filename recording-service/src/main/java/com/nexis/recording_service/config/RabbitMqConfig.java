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

    public static final String CODE_QUEUE = "nexis.code.queue";
    public static final String CHAT_QUEUE = "nexis.chat.queue";
}
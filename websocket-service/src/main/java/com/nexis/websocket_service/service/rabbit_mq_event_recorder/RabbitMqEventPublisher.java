package com.nexis.websocket_service.service.rabbit_mq_event_recorder;

import com.nexis.websocket_service.config.RabbitMqConfig;
import com.nexis.websocket_service.payload.ChatMessage;
import com.nexis.websocket_service.payload.CodeOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RabbitMqEventPublisher {

    private final RabbitTemplate rabbitTemplate;


    public void publishCodeEvent(UUID workspaceId, CodeOperation codeEventChanges) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.HASH_EXCHANGE,
                workspaceId.toString(), // The routing key for the hash exchange
                codeEventChanges,
                message -> {
                  // header for the Recording Service
                    message.getMessageProperties().setHeader("workspaceId", workspaceId.toString());
                    return message;
                }
        );
    }

    public void publishChatEvent(ChatMessage chatEventChanges) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.TOPIC_EXCHANGE,
                RabbitMqConfig.CHAT_ROUTING_KEY,
                chatEventChanges
        );
    }
}
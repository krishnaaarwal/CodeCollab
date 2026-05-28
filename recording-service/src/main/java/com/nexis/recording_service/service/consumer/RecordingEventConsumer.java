package com.nexis.recording_service.service.consumer;

import com.nexis.recording_service.config.RabbitMqConfig;
import com.nexis.recording_service.entity.payload.ChatMessage;
import com.nexis.recording_service.entity.payload.CodeOperation;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RecordingEventConsumer {

    @RabbitListener(queues = RabbitMqConfig.CHAT_QUEUE)
    public void consumeChatMessages(ChatMessage message) {
        // 1. Explicitly receive a ChatMessage object
        // 2. Map it to SessionEventsEntity, setting eventType = "CHAT_MESSAGE"
        // 3. Save via Repository
    }

    @RabbitListener(queues = RabbitMqConfig.CODE_QUEUE)
    public void consumeCodeOperations(CodeOperation operation) {
        // 1. Explicitly receive a CodeOperation object
        // 2. Map it to SessionEventsEntity, setting eventType = "CODE_CHANGE"
        // 3. Pass the operation object directly into the entity's payload field
        // 4. Save via Repository
    }

}

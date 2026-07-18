package com.nexis.recording_service.service.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexis.recording_service.config.RabbitMqConfig;
import com.nexis.recording_service.entity.SessionEventsEntity;
import com.nexis.recording_service.entity.payload.ChatMessage;
import com.nexis.recording_service.entity.payload.CodeOperation;
import com.nexis.recording_service.repository.SessionEventsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecordingEventConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final SessionEventsRepository sessionEventsRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMqConfig.CHAT_QUEUE)
    public void consumeChatMessages(ChatMessage message) {

        UUID workspaceId = message.getWorkspaceId();
        String sessionIdStr = redisTemplate.opsForValue().get("nexis:active-session:" + workspaceId);

        if (sessionIdStr == null) return;

        // 1. Convert ChatMessage to JsonNode
        JsonNode payloadNode = objectMapper.valueToTree(message);

        UUID sessionId = UUID.fromString(sessionIdStr);
        SessionEventsEntity entity = new SessionEventsEntity(
                sessionId,
                "CHAT_MESSAGE",
                message.getUserId(),
                payloadNode,
                LocalDateTime.now()
        );
        sessionEventsRepository.save(entity);
    }

    @RabbitListener(queues = { RabbitMqConfig.CODE_SHARD_0, RabbitMqConfig.CODE_SHARD_1, RabbitMqConfig.CODE_SHARD_2 })
    public void consumeCodeOperations(
            CodeOperation operation,
            @Header("workspaceId") String workspaceId
    ) {

        JsonNode payloadNode = objectMapper.valueToTree(operation);
        String sessionIdStr = redisTemplate.opsForValue().get("nexis:active-session:" + workspaceId);

        if (sessionIdStr == null) return;

        UUID sessionId = UUID.fromString(sessionIdStr);
        SessionEventsEntity entity = new SessionEventsEntity(
                sessionId,
                "CODE_CHANGE",
                operation.getUserId(),
                payloadNode,
                LocalDateTime.now()
        );

        sessionEventsRepository.save(entity);
    }
}
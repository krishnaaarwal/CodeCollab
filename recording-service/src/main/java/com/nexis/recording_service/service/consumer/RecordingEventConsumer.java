package com.nexis.recording_service.service.consumer;

import com.nexis.recording_service.config.RabbitMqConfig;
import com.nexis.recording_service.entity.SessionEventsEntity;
import com.nexis.recording_service.entity.payload.ChatMessage;
import com.nexis.recording_service.entity.payload.CodeOperation;
import com.nexis.recording_service.repository.SessionEventsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
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

    @RabbitListener(queues = RabbitMqConfig.CHAT_QUEUE)
    public void consumeChatMessages(ChatMessage message) {

        UUID workspaceId = message.getWorkspaceId();
        String sessionIdStr = redisTemplate.opsForValue().get("nexis:active-session:" + workspaceId);

        if (sessionIdStr == null) return;

        UUID sessionId = UUID.fromString(sessionIdStr);
        SessionEventsEntity sessionEventsEntity = new SessionEventsEntity(
                sessionId,
                "CHAT_MESSAGE",
                message.getUserId(),
                message,
                LocalDateTime.now()
        );
        sessionEventsRepository.save(sessionEventsEntity);
    }


    @RabbitListener(queues = { RabbitMqConfig.CODE_SHARD_0, RabbitMqConfig.CODE_SHARD_1, RabbitMqConfig.CODE_SHARD_2 })
    public void consumeCodeOperations(
            CodeOperation operation,
            @Header("workspaceId") String workspaceId
    ) {

        String sessionIdStr = redisTemplate.opsForValue().get("nexis:active-session:" + workspaceId);

        if (sessionIdStr == null) return;

        UUID sessionId = UUID.fromString(sessionIdStr);
        SessionEventsEntity sessionEventsEntity = new SessionEventsEntity(
                sessionId,
                "CODE_CHANGE",
                operation.getUserId(),
                operation,
                LocalDateTime.now()
        );
        sessionEventsRepository.save(sessionEventsEntity);
    }

}

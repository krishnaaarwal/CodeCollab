package com.nexis.websocket_service.service.rabbit_mq_event_recorder;

import com.nexis.websocket_service.config.RabbitMqConfig;
import com.nexis.websocket_service.payload.FileEventDto;
import com.nexis.websocket_service.service.pub_sub.RedisMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileEventConsumer {

    private final RedisMessagePublisher redisMessagePublisher;

    @RabbitListener(queues = RabbitMqConfig.FILE_QUEUE)
    public void handlefileEvent(FileEventDto event) {
        log.info("Ingested file event from Storage Engine: {} for file {}", event.eventType(), event.fileId());

        // Push to Redis backplane.
        // This guarantees ALL horizontally scaled WebSocket instances broadcast to their respective clients.
        String redisTopic = "nexis:workspace:" + event.workspaceId() + ":file";
        redisMessagePublisher.publish(redisTopic, event);
    }
}
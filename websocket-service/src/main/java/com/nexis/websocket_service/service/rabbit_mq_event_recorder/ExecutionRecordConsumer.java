package com.nexis.websocket_service.service.rabbit_mq_event_recorder;

import com.nexis.websocket_service.config.RabbitMqConfig;
import com.nexis.websocket_service.payload.ExecutionResultPayload;
import com.nexis.websocket_service.service.pub_sub.RedisMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExecutionRecordConsumer {

    // 1. Swap this dependency
    private final RedisMessagePublisher redisMessagePublisher;

    @RabbitListener(queues = RabbitMqConfig.RESULT_QUEUE)
    public void consumeExecutionResult(ExecutionResultPayload result) {
        log.info("Received execution result for workspace: {}", result.getWorkspaceId());

        // 2. Publish to the Redis channel pattern your subscriber expects
        redisMessagePublisher.publish(
                "nexis:workspace:" + result.getWorkspaceId() + ":terminal",
                result
        );
    }
}
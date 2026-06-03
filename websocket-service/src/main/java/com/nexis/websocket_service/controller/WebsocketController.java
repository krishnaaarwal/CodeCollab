package com.nexis.websocket_service.controller;


import com.nexis.websocket_service.payload.*;
import com.nexis.websocket_service.service.OperationalTransformService;
import com.nexis.websocket_service.service.pub_sub.RedisMessagePublisher;
import com.nexis.websocket_service.service.rabbit_mq_event_recorder.RabbitMqEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Controller// Controller for Websocket , RestController for RestAPI
public class WebsocketController {

    private final OperationalTransformService operationalTransformService;
    private final RedisMessagePublisher redisMessagePublisher;
    private final RabbitMqEventPublisher rabbitMqEventPublisher;

    @MessageMapping("/workspace/{workspaceId}/code")
    public void handleCodeChange(@Payload CodeOperation code, @DestinationVariable UUID workspaceId){   //DestinationVariable instead of Path Variable

        log.info("Code change received | workspace: {} | op: {} | position: {}",
                workspaceId,
                code.getOperationType(),
                code.getPosition()
        );

        CodeOperation transformedOp = operationalTransformService.processOperation(workspaceId,code);

        redisMessagePublisher.publish("nexis:workspace:" + workspaceId + ":code"
                ,transformedOp);

        rabbitMqEventPublisher.publishCodeEvent(workspaceId,transformedOp);

    }

    @MessageMapping("/workspace/{workspaceId}/cursor")
    public void handleCursorChange(@Payload CursorPayload cursor,@DestinationVariable UUID workspaceId){
        log.info("Cursor move | workspace: {} | user: {} | line: {} | col: {}",
                workspaceId,
                cursor.getUserId(),
                cursor.getLine(),
                cursor.getCharacterIndex()
        );

        redisMessagePublisher.publish("nexis:workspace:"+workspaceId+ ":cursor"
                ,cursor);

    }

    @MessageMapping("/workspace/{workspaceId}/chat")
    public void handleChat(@Payload ChatMessage chat, @DestinationVariable UUID workspaceId){
        log.info("Chat message | workspace: {} | user: {} ",
                workspaceId,
                chat.getUserId()
        );

        redisMessagePublisher.publish("nexis:workspace:"+workspaceId+":chat"
                ,chat);

        rabbitMqEventPublisher.publishChatEvent(chat);
    }

    @MessageMapping("/workspace/{workspaceId}/typing")
    public void handleTyping(@Payload TypingPayload typing, @DestinationVariable UUID workspaceId) {
        redisMessagePublisher.publish("nexis:workspace:" + workspaceId + ":typing", typing);
    }

    @MessageMapping("/private")
    public void handlePrivateMessage(@Payload PrivateMessagePayload message) {
        log.info("Private message from {} to {}", message.getSenderId(), message.getReceiverId());

        redisMessagePublisher.publish("nexis:user:" + message.getReceiverId() + ":private", message);
    }
}

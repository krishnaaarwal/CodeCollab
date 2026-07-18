package com.nexis.websocket_service.service.pub_sub;

import com.nexis.websocket_service.config.RabbitMqConfig;
import com.nexis.websocket_service.payload.ExecutionResultPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessageSubscriber implements MessageListener {
    //                                          Spring Data Redis interface
    //                                          It has ONE method: onMessage()
    //                                          Redis calls this whenever a message arrives


    private final SimpMessagingTemplate messagingTemplate;      //"Simp" = Simple Messaging Protocol.
                                                                // SimpMessagingTemplate allows us to programmatically send STOMP messages

    @Override
    public void onMessage(Message message, byte[] pattern) {   // This method fires on EVERY instance whenever Redis broadcasts
        try {

            String publishedMessage = new String(message.getBody());
            String channel = new String(message.getChannel());
            String[] parts = channel.split(":");  // Split by ":" → ["nexis", "workspace", "123", "cursor"]

            String destination;

            // Pattern: nexis:workspace:{id}:{type}
            if(parts[1].equals("workspace")){
                String workspaceId = parts[2];
                String channelType = parts[3];
                destination = "/topic/workspace/" + workspaceId + "/"+ channelType;

                messagingTemplate.convertAndSend(destination, publishedMessage);
            }
            // Pattern: nexis:user:{id}:private
            else if (parts[1].equals("user")) {
                String userId = parts[2];

                // BYPASS convertAndSendToUser. Send directly to a user-scoped topic.
                messagingTemplate.convertAndSend(
                        "/topic/user/" + userId + "/private",
                        publishedMessage
                );
//                messagingTemplate.convertAndSendToUser(
//                        userId,              // Spring finds this user's session
//                        "/queue/private",    // client subscribed to /user/queue/private
//                        publishedMessage
//                );

            }else {
                log.warn("Unrecognized Redis channel pattern received: {}", channel);
            }



        } catch (Exception e) {
            log.error("CRITICAL: Failed to process incoming Redis message. Error: {}", e.getMessage(), e);
        }
    }

}
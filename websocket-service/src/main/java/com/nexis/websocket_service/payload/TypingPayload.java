package com.nexis.websocket_service.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.UUID;

@Data
public class TypingPayload {
    private UUID userId;
    @JsonProperty("isTyping")
    private boolean isTyping;
}

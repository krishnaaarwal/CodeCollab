package com.nexis.recording_service.entity.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ChatMessage {
    private UUID userId;
    private UUID workspaceId;  //which workspace it belongs to
    private LocalDateTime time = LocalDateTime.now();
    private String message;
}

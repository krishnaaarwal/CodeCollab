package com.nexis.websocket_service.payload;

import com.nexis.websocket_service.config.type.StatusType;
import lombok.Data;

import java.util.UUID;

@Data
public class ExecutionResultPayload {
    private UUID id; // jobId
    private UUID workspaceId;
    private UUID userId;
    private StatusType status;
    private String output;
    private String error;
}
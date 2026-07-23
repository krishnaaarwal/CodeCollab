package com.nexis.websocket_service.payload;

import java.util.UUID;

public record FileEventDto(
        String eventType,
        UUID workspaceId,
        UUID fileId,
        String newName,
        String timestamp
) {}
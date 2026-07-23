package com.nexis.storage_service.dto;

import java.util.UUID;

public record FileEventDto(
        String eventType,
        UUID workspaceId,
        UUID fileId,
        String newName,
        String timestamp
) {}
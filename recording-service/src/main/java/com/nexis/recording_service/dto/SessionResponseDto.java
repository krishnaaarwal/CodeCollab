package com.nexis.recording_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionResponseDto(
         UUID id,
         UUID workspaceId,
        LocalDateTime startedAt,
         LocalDateTime endedAt,
         java.time.Duration duration
) {
}

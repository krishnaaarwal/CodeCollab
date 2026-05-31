package com.nexis.recording_service.dto;

import com.nexis.recording_service.entity.SessionEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionResponseDto(
        UUID id,
        UUID workspaceId,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long durationSeconds
) {

    public SessionResponseDto(SessionEntity entity) {
        this(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getStartedAt(),
                entity.getEndedAt(),
                entity.getEndedAt() != null ?
                        entity.getDuration().toSeconds() :
                        java.time.Duration.between(entity.getStartedAt(), LocalDateTime.now()).toSeconds()
        );
    }
}

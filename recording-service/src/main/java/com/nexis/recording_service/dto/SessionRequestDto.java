package com.nexis.recording_service.dto;

import java.util.List;
import java.util.UUID;

public record SessionRequestDto(
        UUID workspaceId,
        List<UUID> participants
) {
}

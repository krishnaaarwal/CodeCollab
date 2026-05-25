package com.nexis.storage_service.dto;

import java.util.UUID;

public record FileResponseDto(
        String url,
        UUID fileId
) {
}

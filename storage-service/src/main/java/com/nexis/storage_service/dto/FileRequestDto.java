package com.nexis.storage_service.dto;

import java.math.BigInteger;
import java.util.UUID;

public record FileRequestDto(
        UUID workspaceId,
        String fileName,
        Long size
) {
}

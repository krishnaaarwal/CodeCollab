package com.nexis.storage_service.dto;

import java.util.UUID;

public record UploadCompleteDto(
        UUID workspaceId,
        UUID fileId,
        int versionNum,
        String fileName,
        long sizeBytes
) {}

package com.nexis.storage_service.dto;

import java.util.UUID;

public record FileRenameRequestDto(
        UUID workspaceId,
        String newName
) {}
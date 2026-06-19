package com.nexis.storage_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FileResponseDto(
        UUID fileId,
        String fileName,
        Long fileSize,
        String fileType,
        Integer currentVersion,
        String url
) {

    public FileResponseDto(String url, UUID fileId) {
        this(fileId, null, null, null, null, url);
    }


    public FileResponseDto(UUID fileId, String fileName, Long fileSize, String fileType, Integer currentVersion) {
        this(fileId, fileName, fileSize, fileType, currentVersion, null);
    }
}
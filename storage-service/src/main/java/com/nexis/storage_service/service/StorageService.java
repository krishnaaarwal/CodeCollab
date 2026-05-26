package com.nexis.storage_service.service;

import com.nexis.storage_service.dto.FileRequestDto;
import com.nexis.storage_service.dto.FileResponseDto;
import com.nexis.storage_service.dto.UploadCompleteDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface StorageService {
    void uploadComplete(UploadCompleteDto dto, UUID userId);

    FileResponseDto downloadFile(UUID id, UUID userId);

    FileResponseDto uploadFile(FileRequestDto fileRequestDto, UUID userId);
}

package com.nexis.storage_service.service;

import com.nexis.storage_service.dto.FileRequestDto;
import com.nexis.storage_service.dto.FileResponseDto;

import java.util.UUID;

public interface StorageService {
    FileResponseDto uploadFile(FileRequestDto fileRequestDto);

    FileResponseDto downloadFile(UUID id);
}

package com.nexis.storage_service.service.impl;

import com.nexis.storage_service.dto.FileRequestDto;
import com.nexis.storage_service.dto.FileResponseDto;
import com.nexis.storage_service.service.StorageService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StorageServiceImplementation implements StorageService {
    @Override
    public FileResponseDto uploadFile(FileRequestDto fileRequestDto) {
        return null;
    }

    @Override
    public FileResponseDto downloadFile(UUID id) {
        return null;
    }
}

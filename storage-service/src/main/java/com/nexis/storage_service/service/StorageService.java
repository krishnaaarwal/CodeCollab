package com.nexis.storage_service.service;

import com.nexis.storage_service.dto.FileRenameRequestDto;
import com.nexis.storage_service.dto.FileRequestDto;
import com.nexis.storage_service.dto.FileResponseDto;
import com.nexis.storage_service.dto.UploadCompleteDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface StorageService {
    void uploadComplete(UploadCompleteDto dto, UUID userId);

    FileResponseDto downloadFile(UUID id, UUID userId);

    FileResponseDto uploadFile(FileRequestDto fileRequestDto, UUID userId);

    List<FileResponseDto> getFilesByWorkspace(UUID workspaceId, UUID userUuid);

    void renameFile(UUID fileId, FileRenameRequestDto dto, UUID userId);

    void deleteFile(UUID fileId, UUID workspaceId, UUID userId);
}

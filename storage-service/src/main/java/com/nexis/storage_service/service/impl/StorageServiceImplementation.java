package com.nexis.storage_service.service.impl;

import com.nexis.storage_service.client.AuthServiceClient;
import com.nexis.storage_service.config.type.FileStatus;
import com.nexis.storage_service.dto.FileRequestDto;
import com.nexis.storage_service.dto.FileResponseDto;
import com.nexis.storage_service.dto.UploadCompleteDto;
import com.nexis.storage_service.entity.FileEntity;
import com.nexis.storage_service.entity.FileVersionEntity;
import com.nexis.storage_service.repository.FileRepository;
import com.nexis.storage_service.repository.FileVersionRepository;
import com.nexis.storage_service.service.StorageService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageServiceImplementation implements StorageService {

    private final FileRepository fileRepository;
    private final FileVersionRepository fileVersionRepository;
    private final AuthServiceClient authServiceClient;
    private final MinioClient minioClient;

    @Override
    @Transactional
    public FileResponseDto uploadFile(FileRequestDto dto, UUID userId) {
        // 1. Check workspace bounds via Feign
        boolean hasAccess = authServiceClient.isWorkspaceMember(dto.workspaceId(), userId);
        if (!hasAccess) throw new SecurityException("Forbidden");

        Optional<FileEntity> existingFileOpt = fileRepository.findByWorkspaceIdAndFileName(dto.workspaceId(), dto.fileName());

        UUID fileId;
        int versionToUpload;

        if (existingFileOpt.isEmpty()) {
            fileId = UUID.randomUUID();
            versionToUpload = 1;

            // NOTE: We do NOT insert anything into the files table yet!
            // If the upload fails, this file simply never existed. No corrupted state.
        } else {
            FileEntity existingFile = existingFileOpt.get();
            fileId = existingFile.getId();
            versionToUpload = existingFile.getCurrentVersion() + 1;

            // NOTE: Leave existingFile completely alone here. Do not increment or save it yet!
        }

        String targetStorageKey = "workspaces/" + dto.workspaceId() + "/files/" + fileId + "/version_" + versionToUpload;

        // Log the unverified file instance to disk as PENDING
        FileVersionEntity pendingVersion = FileVersionEntity.builder()
                .id(UUID.randomUUID())
                .fileId(fileId)
                .fileSize(dto.size())
                .version(versionToUpload)
                .storageKey(targetStorageKey)
                .fileStatus(FileStatus.PENDING) // Securely flagged
                .createdAt(LocalDateTime.now())
                .createdBy(userId)
                .build();

        fileVersionRepository.save(pendingVersion);

        GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket("nexis-workspaces")
                .object(targetStorageKey)
                .expiry(15 * 60) // 15 minutes in seconds
                .build();

        String presignedUrl;
        try {
            presignedUrl = minioClient.getPresignedObjectUrl(args);
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL for storage key: {}", targetStorageKey, e);
            throw new RuntimeException("Storage engine infrastructure failure", e);
        }


        return new FileResponseDto(presignedUrl, fileId);
    }

    @Transactional
    @Override
    public void uploadComplete(UploadCompleteDto dto, UUID userId) {

        String expectedStorageKey = "workspaces/" + dto.workspaceId() + "/files/" + dto.fileId() + "/version_" + dto.versionNum();

        FileVersionEntity pendingVersion = fileVersionRepository
                .findByFileIdAndVersionAndStorageKey(dto.fileId(), dto.versionNum(), expectedStorageKey)
                .orElseThrow(() -> new IllegalArgumentException("Upload intent log not found or tampered with."));

        if (pendingVersion.getFileStatus() != FileStatus.PENDING) {
            throw new IllegalStateException("Version state has already been processed.");
        }

        pendingVersion.setFileStatus(FileStatus.ACTIVE);
        fileVersionRepository.save(pendingVersion);

        // Lock the primary metadata row to prevent race conditions
        Optional<FileEntity> currentFileOpt = fileRepository.findByIdForUpdate(dto.fileId());

        if (currentFileOpt.isEmpty()) {
            String fileType = extractExtension(dto.fileName());

            FileEntity brandNewFile = FileEntity.builder()
                    .id(dto.fileId())
                    .workspaceId(dto.workspaceId())
                    .fileName(dto.fileName())
                    .fileSize(dto.sizeBytes())
                    .fileType(fileType)
                    .currentVersion(dto.versionNum())
                    .storageKey(expectedStorageKey)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            fileRepository.save(brandNewFile);
        } else {
            FileEntity activeFile = currentFileOpt.get();

            activeFile.setCurrentVersion(dto.versionNum());
            activeFile.setStorageKey(expectedStorageKey);
            activeFile.setFileSize(dto.sizeBytes());
            activeFile.setUpdatedAt(LocalDateTime.now());

            fileRepository.save(activeFile);
        }
    }

    private String extractExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "txt";
    }

    @Override
    @Transactional(readOnly = true) // Tells Hibernate to bypass dirty checking calculations for performance
    public FileResponseDto downloadFile(UUID fileId, UUID userId) {

        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found."));


        boolean hasAccess = authServiceClient.isWorkspaceMember(fileEntity.getWorkspaceId(),userId);
        if (!hasAccess) throw new SecurityException("Forbidden");


        String targetStorageKey = fileEntity.getStorageKey();

        GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket("nexis-workspaces")
                .object(targetStorageKey)
                .expiry(15 * 60) // 15 minutes in seconds
                .build();

        String presignedUrl;
        try {
            presignedUrl = minioClient.getPresignedObjectUrl(args);
        } catch (Exception e) {
        log.error("Failed to generate presigned upload URL for storage key: {}", targetStorageKey, e);
        throw new RuntimeException("Storage engine infrastructure failure", e);
    }

        return new FileResponseDto(presignedUrl,fileId);
    }

}

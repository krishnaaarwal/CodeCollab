package com.nexis.storage_service.service.impl;

import com.nexis.storage_service.client.AuthServiceClient;
import com.nexis.storage_service.config.type.FileStatus;
import com.nexis.storage_service.dto.FileRequestDto;
import com.nexis.storage_service.dto.FileResponseDto;
import com.nexis.storage_service.dto.UploadCompleteDto;
import com.nexis.storage_service.entity.FileEntity;
import com.nexis.storage_service.entity.FileVersionEntity;
import com.nexis.storage_service.exception.FileVersionConflictException;
import com.nexis.storage_service.exception.UploadIntentNotFoundException;
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
import java.util.List;
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
        log.info("Initiating file upload intent. Workspace: {}, File: {}, User: {}",
                dto.workspaceId(), dto.fileName(), userId);

        boolean hasAccess = authServiceClient.isWorkspaceMember(dto.workspaceId(), userId);
        if (!hasAccess) {
            log.warn("Access Denied: User {} attempted unauthorized upload to workspace {}", userId, dto.workspaceId());
            throw new SecurityException("Forbidden: Invalid workspace membership.");
        }

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
        log.debug("Pre-calculated storage parameters: File ID: {}, Next Version: {}", fileId, versionToUpload);

        String targetStorageKey = "workspaces/" + dto.workspaceId() + "/files/" + fileId + "/version_" + versionToUpload;

        // Log the unverified file instance to disk as PENDING
        FileVersionEntity pendingVersion = FileVersionEntity.builder()
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


        try {
             String presignedUrl = minioClient.getPresignedObjectUrl(args);
            log.info("Successfully generated presigned PUT URL for File ID: {}, Version: {}", fileId, versionToUpload);
            return new FileResponseDto(presignedUrl, fileId);
        } catch (Exception e) {
            log.error("S3 SDK Engine failure while crafting upload token for Key: {}", targetStorageKey, e);
            throw new RuntimeException("Storage engine infrastructure failure", e);
        }

    }

    @Transactional
    @Override
    public void uploadComplete(UploadCompleteDto dto, UUID userId) {

        log.info("Received upload confirmation callback from client. File ID: {}, Version: {}, User: {}",
                dto.fileId(), dto.versionNum(), userId);

        String expectedStorageKey = "workspaces/" + dto.workspaceId() + "/files/" + dto.fileId() + "/version_" + dto.versionNum();

        FileVersionEntity pendingVersion = fileVersionRepository
                .findByFileIdAndVersionAndStorageKey(dto.fileId(), dto.versionNum(), expectedStorageKey)
                .orElseThrow(() -> {
                    log.error("Malicious or corrupt confirmation: Intent log missing for File: {}, Version: {}", dto.fileId(), dto.versionNum());
                    return new UploadIntentNotFoundException("Upload intent log not found or tampered with.");
                });

        if (pendingVersion.getFileStatus() != FileStatus.PENDING) {
            log.warn("Idempotency Block: Upload complete already processed for File: {}, Version: {}", dto.fileId(), dto.versionNum());
            throw new FileVersionConflictException("Version state has already been processed.");
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
            log.info("Successfully provisioned new root file entry. File ID: {}, Name: {}", dto.fileId(), dto.fileName());
        } else {
            FileEntity activeFile = currentFileOpt.get();

            activeFile.setCurrentVersion(dto.versionNum());
            activeFile.setStorageKey(expectedStorageKey);
            activeFile.setFileSize(dto.sizeBytes());
            activeFile.setUpdatedAt(LocalDateTime.now());

            fileRepository.save(activeFile);
            log.info("Successfully advanced master record pointer to Version: {} for File ID: {}", dto.versionNum(), dto.fileId());
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

      log.info("Processing download link request for File ID: {} by User: {}", fileId, userId);

        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.warn("Download target missing: File ID {} does not exist", fileId);
                    return new IllegalArgumentException("File not found.");
                });

        boolean hasAccess = authServiceClient.isWorkspaceMember(fileEntity.getWorkspaceId(), userId);
        if (!hasAccess) {
            log.warn("Security Breach: User {} attempted unauthorized read access on File: {}", userId, fileId);
            throw new SecurityException("Forbidden");
        }


        String targetStorageKey = fileEntity.getStorageKey();

        GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket("nexis-workspaces")
                .object(targetStorageKey)
                .expiry(15 * 60) // 15 minutes in seconds
                .build();

        try {
            String presignedUrl = minioClient.getPresignedObjectUrl(args);
            log.info("Successfully generated presigned GET URL for File ID: {}, Storage Key: {}", fileId, fileEntity.getStorageKey());
            return new FileResponseDto(presignedUrl, fileId);
        } catch (Exception e) {
            // FIXED: Cleaned your copy-paste upload logging artifact
            log.error("S3 SDK Engine failure while crafting download token for Key: {}", fileEntity.getStorageKey(), e);
            throw new RuntimeException("Storage engine infrastructure failure", e);
        }

    }

    @Override
    @Transactional(readOnly = true)
    public List<FileResponseDto> getFilesByWorkspace(UUID workspaceId, UUID userId) {
        log.info("Fetching complete file tree metadata for Workspace ID: {} by User: {}", workspaceId, userId);

        boolean hasAccess = authServiceClient.isWorkspaceMember(workspaceId, userId);
        if (!hasAccess) {
            log.warn("Unauthorized access attempt on Workspace tree: User: {}", userId);
            throw new SecurityException("Forbidden: You do not have access to this workspace.");
        }

        List<FileEntity> files = fileRepository.findByWorkspaceId(workspaceId);

        return files.stream()
                .map(file -> new FileResponseDto(
                        file.getId(),
                        file.getFileName(),
                        file.getFileSize(),
                        file.getFileType(),
                        file.getCurrentVersion()
                ))
                .toList();
    }

}

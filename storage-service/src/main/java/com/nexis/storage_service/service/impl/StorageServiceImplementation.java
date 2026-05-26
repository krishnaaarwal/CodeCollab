package com.nexis.storage_service.service.impl;

import com.nexis.storage_service.client.AuthServiceClient;
import com.nexis.storage_service.config.type.FileStatus;
import com.nexis.storage_service.dto.FileRequestDto;
import com.nexis.storage_service.dto.FileResponseDto;
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
    public void completeUpload(UUID workspaceId, UUID fileId, int versionNum, String fileName, long sizeBytes) {
        // 1. Fetch the PENDING historical version record from your repository layer
        // 2. Flip its status from FileStatus.PENDING to FileStatus.ACTIVE

        // 3. Query your FileRepository using findByIdForUpdate(fileId) to lock the master row
        // 4. If it's a completely new file (Optional is empty), build and save a brand new FileEntity
        // 5. If it's an update, advance the version counter, update size, and point the active storageKey to the new version key
    }

    @Override
    @Transactional(readOnly = true) // Tells Hibernate to bypass dirty checking calculations for performance
    public FileResponseDto downloadFile(UUID fileId, UUID userId) {
        // 1. Fetch the active master metadata row from your FileRepository
        // 2. Use your AuthServiceClient Feign client to verify if the current userId has access to that workspace

        // 3. Use GetPresignedObjectUrlArgs with Method.GET to request a download link from MinIO
        // 4. Return the presigned GET string packaged inside a clean FileResponseDto
    }


}

package com.nexis.storage_service.service.impl;

import com.nexis.storage_service.client.AuthServiceClient;
import com.nexis.storage_service.dto.FileRequestDto;
import com.nexis.storage_service.dto.FileResponseDto;
import com.nexis.storage_service.entity.FileEntity;
import com.nexis.storage_service.entity.FileVersionEntity;
import com.nexis.storage_service.repository.FileRepository;
import com.nexis.storage_service.repository.FileVersionRepository;
import com.nexis.storage_service.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageServiceImplementation implements StorageService {

    private final FileRepository fileRepository;
    private final FileVersionRepository fileVersionRepository;
    private final AuthServiceClient authServiceClient;
//    private final MinioClient minioClient;

    @Override
    @Transactional
    public FileResponseDto uploadFile(FileRequestDto dto, UUID userId) {

        // 1. DISTRIBUTED VALIDATION
        boolean hasAccess = authServiceClient.isWorkspaceMember(dto.workspaceId(), userId);
        if (!hasAccess) {
            throw new SecurityException("User does not have access to this workspace.");
        }

        // 2. TARGET DIRECT QUERY
        Optional<FileEntity> existingFileOpt = fileRepository.findByWorkspaceIdAndFileName(dto.workspaceId(), dto.fileName());

        String targetStorageKey;
        UUID fileId;
        int nextVersion;

        if (existingFileOpt.isEmpty()) {
            // 3. LOGIC FOR A COMPLETELY NEW FILE
            fileId = UUID.randomUUID(); // Pre-generate ID to compute unique key safely
            nextVersion = 1;
            UUID workspaceId = dto.workspaceId();

            // Key format standard: workspaces/{workspaceId}/files/{fileId}/version_{versionNum}
            targetStorageKey = "workspaces/"+workspaceId+"/files/"+fileId+"/version_"+nextVersion;

            String fileName = dto.fileName();
            String fileType = "txt"; // Default fallback

            int lastDotIndex = fileName.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
                fileType = fileName.substring(lastDotIndex + 1).toLowerCase();
            }

            FileEntity fileEntity = FileEntity.builder()
                    .id(fileId).workspaceId(workspaceId).fileName(dto.fileName()).fileSize(dto.size())
                    .fileType(fileType)
                    .currentVersion(nextVersion).storageKey(targetStorageKey)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

            fileRepository.save(fileEntity);

            FileVersionEntity fileVersionEntity = FileVersionEntity.builder()
                    .id(UUID.randomUUID()).fileId(fileId).fileSize(dto.size())
                    .version(nextVersion).storageKey(targetStorageKey).createdAt(LocalDateTime.now())
                    .createdBy(userId).build();

        } else {
            // 4. LOGIC FOR UPDATING AN EXISTING FILE (Pessimistic Locking protects this path)
            FileEntity existingFile = fileRepository.findByIdForUpdate(existingFileOpt.get().getId())
                    .orElseThrow(() -> new IllegalStateException("File lock contention failed."));

            fileId = existingFile.getId();
            nextVersion = existingFile.getCurrentVersion() + 1;
            UUID workspaceId = existingFile.getWorkspaceId();

            targetStorageKey = "workspaces/"+workspaceId+"/files/"+fileId+"/version_"+nextVersion;

            existingFile.setCurrentVersion(nextVersion);
            existingFile.setStorageKey(targetStorageKey);
            existingFile.setFileSize(dto.size());
            fileRepository.save(existingFile);


            FileVersionEntity fileVersionEntity = FileVersionEntity.builder()
                    .id(UUID.randomUUID()).fileId(fileId).fileSize(dto.size())
                    .version(nextVersion).storageKey(targetStorageKey).createdAt(LocalDateTime.now())
                    .createdBy(userId).build();

            fileVersionRepository.save(fileVersionEntity);

        }

        // 5. GENERATE PRESIGNED URL VIA S3 SDK
        // TODO: Talk to minioClient using targetStorageKey to request a Presigned PUT URL valid for 15 minutes.
        String presignedUrl = "PRE_SIGNED_URL_FROM_MINIO_CLIENT";

        return new FileResponseDto(presignedUrl, fileId);
    }

    @Override
    public FileResponseDto downloadFile(UUID id, UUID userId) {
        return null;
    }


}

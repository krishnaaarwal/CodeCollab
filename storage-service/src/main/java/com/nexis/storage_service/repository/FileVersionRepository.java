package com.nexis.storage_service.repository;

import com.nexis.storage_service.entity.FileVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileVersionRepository extends JpaRepository<FileVersionEntity,UUID> {

    Optional<FileVersionEntity> findByFileIdAndVersionAndStorageKey(UUID uuid, int i, String expectedStorageKey);
    // Add this new method signature:
    Optional<FileVersionEntity> findFirstByFileIdAndVersionAndStorageKeyOrderByCreatedAtDesc(
            UUID fileId,
            Integer version,
            String storageKey
    );
}

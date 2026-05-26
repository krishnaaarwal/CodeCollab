package com.nexis.storage_service.repository;

import com.nexis.storage_service.entity.FileEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    List<FileEntity> findByWorkspaceId(UUID workspaceId);

    // Prevents Race Conditions: Locks the row at the database level during a version modification
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FileEntity f WHERE f.id = :id")
    Optional<FileEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<FileEntity> findByWorkspaceIdAndFileName(UUID uuid, String s);
}

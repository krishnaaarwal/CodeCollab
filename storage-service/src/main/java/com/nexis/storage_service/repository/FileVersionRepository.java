package com.nexis.storage_service.repository;

import com.nexis.storage_service.entity.FileVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FileVersionRepository extends JpaRepository<FileVersionEntity,UUID> {
}

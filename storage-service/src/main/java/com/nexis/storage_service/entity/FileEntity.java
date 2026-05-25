package com.nexis.storage_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID workspaceId;
    private String fileName;
    private BigInteger fileSize;
    private String fileType;
    private Integer currentVersion;
    private String storageKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

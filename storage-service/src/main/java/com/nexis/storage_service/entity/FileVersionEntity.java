package com.nexis.storage_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class FileVersionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID fileId;
    private Long fileSize;
    private Integer version;
    private String storageKey;
    private LocalDateTime createdAt;
    private UUID createdBy;                     //User triggering this save
}

package com.nexis.storage_service.entity;

import com.nexis.storage_service.config.type.FileStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "file_versions", indexes = {
        @Index(
                name = "idx_file_version_lookup",
                columnList = "file_id, version" // FIXED: Uses columnList with physical snake_case column names
        )
})
public class FileVersionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID fileId;
    private Long fileSize;
    private Integer version;
    private String storageKey;
    private FileStatus fileStatus;
    private LocalDateTime createdAt;
    private UUID createdBy;                     //User triggering this save
}

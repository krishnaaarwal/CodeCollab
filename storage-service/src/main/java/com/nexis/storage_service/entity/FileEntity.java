package com.nexis.storage_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "files", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_workspace_file",
                columnNames = {"workspaceId", "fileName"} // Must match your exact Java field names or explicit @Column mappings
        )
})
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID workspaceId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private Integer currentVersion;
    private String storageKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

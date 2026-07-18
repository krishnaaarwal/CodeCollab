package com.nexis.execution_service.entity;

import com.nexis.execution_service.config.type.CodeLanguage;
import com.nexis.execution_service.config.type.StatusType;
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
@Table(name = "execution_jobs", indexes = {
        @Index(name = "idx_workspace_id", columnList = "workspaceId"),
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_status", columnList = "status")
})
public class ExecutionJob {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;
    private UUID workspaceId;

    @Enumerated(EnumType.STRING)
    private CodeLanguage codeLanguage;

    @Column(columnDefinition = "TEXT")
    private String code;

    @Enumerated(EnumType.STRING)
    private StatusType status;

    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(columnDefinition = "TEXT")
    private String error;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}

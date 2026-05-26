package com.nexis.auth_service.entity;

import com.nexis.auth_service.config.type.ProviderType;
import com.nexis.auth_service.config.type.WorkspaceRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_provider", columnList = "providerId, providerType")
})
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;
    private String fullname;
    private String avatar;
    private String providerId;

    @Enumerated(EnumType.STRING)
    private ProviderType providerType;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkspaceMemberEntity> workspaceMemberList = new ArrayList<>();
}
package com.nexis.auth_service.entity;

import com.nexis.auth_service.config.type.ProviderType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String email;
    private String password;
    private String fullname;
    private String avatar;
    private String providerId;
    private ProviderType providerType;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<WorkspaceMemberEntity> workspaceMemberList = new ArrayList<>();
}

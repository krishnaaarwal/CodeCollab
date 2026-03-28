package com.nexis.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "workspaces")
public class WorkspaceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String name;

    private String description;
    private String visibility;

    @OneToMany(mappedBy = "workspace",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<WorkspaceMemberEntity> members = new ArrayList<>();
}

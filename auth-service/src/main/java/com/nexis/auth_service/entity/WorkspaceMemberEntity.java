package com.nexis.auth_service.entity;

import com.nexis.auth_service.config.type.WorkspaceRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class WorkspaceMemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceEntity workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceRole role;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

}

//BADGE ANALOGY:----
//--------------------------------------------------------
//Imagine a physical building (a Workspace) and a physical person (a User).
//
//The building has a list of who is allowed inside.
//
//To get inside, a User needs a Badge (WorkspaceMemberEntity).
//
//If you (the User) belong to 3 different Workspaces, you don't have one magic badge that changes its data. You literally have 3 separate physical badges on your lanyard.
//
//Badge 1: Belongs to You, belongs to Workspace A, says "OWNER", created Monday.
//
//Badge 2: Belongs to You, belongs to Workspace B, says "MEMBER", created Tuesday.
//
//Badge 3: Belongs to You, belongs to Workspace C, says "ADMIN", created Friday.
//
//Therefore, one single Badge (WorkspaceMemberEntity) points to exactly ONE User and exactly ONE Workspace.
//Why the User Entity is Mandatory Here
//
//Without the UserEntity, the workspace knows when someone joined and what their role is, but it has no idea WHO they are! The WorkspaceMemberEntity is simply the bridge connecting the User to the Workspace.
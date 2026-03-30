package com.nexis.auth_service.dto.workspace;

import com.nexis.auth_service.config.type.WorkspaceRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkspaceMemberResponseDto {
    private UUID userId;
    private String fullname;
    private String email;
    private WorkspaceRole role;
}
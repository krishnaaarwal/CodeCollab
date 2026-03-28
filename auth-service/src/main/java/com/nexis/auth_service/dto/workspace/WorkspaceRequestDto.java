package com.nexis.auth_service.dto.workspace;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkspaceRequestDto {
    private UUID ownerId;
    private String name;
    private String description;
    private String visibility;
}


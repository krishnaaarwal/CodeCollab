package com.nexis.auth_service.dto.workspace;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WorkspaceRequestDto {
    @NotBlank(message = "Workspace name cannot be empty")
    private String name;
    private String description;
    private String visibility;
}


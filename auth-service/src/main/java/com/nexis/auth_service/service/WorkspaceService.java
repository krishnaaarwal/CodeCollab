package com.nexis.auth_service.service;

import com.nexis.auth_service.dto.workspace.WorkspaceRequestDto;
import com.nexis.auth_service.dto.workspace.WorkspaceResponseDto;

import java.util.List;
import java.util.UUID;

public interface WorkspaceService {
    List<WorkspaceResponseDto> getUserWorkspaces();

    WorkspaceResponseDto createWorkspace(WorkspaceRequestDto requestDto);

    WorkspaceResponseDto addWorkspaceMember(UUID id,UUID memberId);

    WorkspaceResponseDto updateWorkspace(UUID id, WorkspaceRequestDto requestDto);
}

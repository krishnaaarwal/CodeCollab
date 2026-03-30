package com.nexis.auth_service.service;

import com.nexis.auth_service.dto.workspace.WorkspaceMemberResponseDto;
import com.nexis.auth_service.dto.workspace.WorkspaceRequestDto;
import com.nexis.auth_service.dto.workspace.WorkspaceResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface WorkspaceService {
    List<WorkspaceResponseDto> getUserWorkspaces();

    WorkspaceResponseDto createWorkspace(WorkspaceRequestDto requestDto);

    WorkspaceResponseDto addWorkspaceMember(UUID id,UUID memberId);

    WorkspaceResponseDto updateWorkspace(UUID id, WorkspaceRequestDto requestDto);

    List<WorkspaceMemberResponseDto> getWorkspaceMembers(UUID workspaceId);

    WorkspaceResponseDto getWorkspaceById(UUID id);

    void deleteUserFromWorkspace(UUID id, UUID memberId);
}

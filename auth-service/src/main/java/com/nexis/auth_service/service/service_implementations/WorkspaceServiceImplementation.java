package com.nexis.auth_service.service.service_implementations;

import com.nexis.auth_service.config.type.WorkspaceRole;
import com.nexis.auth_service.dto.workspace.WorkspaceRequestDto;
import com.nexis.auth_service.dto.workspace.WorkspaceResponseDto;
import com.nexis.auth_service.entity.UserEntity;
import com.nexis.auth_service.entity.WorkspaceEntity;
import com.nexis.auth_service.entity.WorkspaceMemberEntity;
import com.nexis.auth_service.exception.DuplicateMemberException;
import com.nexis.auth_service.exception.ResourceNotFoundException;
import com.nexis.auth_service.repository.UserRepository;
import com.nexis.auth_service.repository.WorkspaceRepository;
import com.nexis.auth_service.security.user_principal.UserPrincipal;
import com.nexis.auth_service.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImplementation implements WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    @PreAuthorize("isAuthenticated()")
    @Override
    @Transactional
    public List<WorkspaceResponseDto> getUserWorkspaces() {
        Authentication authentication= SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UserEntity user = userPrincipal.getUserEntity();

        log.info("Fetching workspaces for User ID: {}", user.getId());

        List<WorkspaceMemberEntity> workspaceMemberEntityList = user.getWorkspaceMemberList();
        List<WorkspaceEntity> workspaceEntityList = workspaceMemberEntityList.stream()
                .map(WorkspaceMemberEntity::getWorkspace).toList();

        return workspaceEntityList.stream().map(
                workspaceEntity -> new WorkspaceResponseDto(
                        workspaceEntity.getId(),
                        workspaceEntity.getOwnerId(),
                        workspaceEntity.getName(),
                        workspaceEntity.getDescription(),
                        workspaceEntity.getVisibility()
                )
        ).toList();
    }

    @PreAuthorize("isAuthenticated()")
    @Override
    @Transactional
    public WorkspaceResponseDto createWorkspace(WorkspaceRequestDto requestDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UserEntity user = userPrincipal.getUserEntity();
        UUID ownerId = user.getId();

        log.info("User ID: {} is creating a new workspace named: '{}'", ownerId, requestDto.getName());

        WorkspaceEntity workspace = WorkspaceEntity.builder()
                .ownerId(ownerId)
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .visibility(requestDto.getVisibility())
                .build();

        workspaceRepository.save(workspace);

        WorkspaceMemberEntity workspaceMember = WorkspaceMemberEntity.builder()
                .workspace(workspace)
                .user(user)
                .role(WorkspaceRole.OWNER)
                .joinedAt(LocalDateTime.now())
                .build();

        workspace.getMembers().add(workspaceMember);
        user.getWorkspaceMemberList().add(workspaceMember);

        log.info("Successfully created Workspace ID: {} with Owner ID: {}", workspace.getId(), ownerId);

        return new WorkspaceResponseDto(
                workspace.getId(),
                workspace.getOwnerId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.getVisibility()
        );
    }

    @PreAuthorize("@workspaceSecurity.isOwnerOrAdmin(#id)")
    @Override
    @Transactional
    public WorkspaceResponseDto addWorkspaceMember(UUID id, UUID memberId) {

        WorkspaceEntity workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + id));

        UserEntity user = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with ID: " + memberId));

        boolean alreadyMember = workspace.getMembers().stream()
                .anyMatch(member -> member.getUser().getId().equals(memberId));

        if (alreadyMember) {
            log.warn("Failed to add member: User ID {} is already in Workspace ID {}", memberId, id);

            throw new DuplicateMemberException("User is already a member of this workspace");
        }

        WorkspaceMemberEntity workspaceMember = WorkspaceMemberEntity.builder()
                .workspace(workspace)
                .user(user)
                .role(WorkspaceRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();

        workspace.getMembers().add(workspaceMember);
        user.getWorkspaceMemberList().add(workspaceMember);

        log.info("Successfully added User ID: {} as MEMBER to Workspace ID: {}", memberId, id);

        return new WorkspaceResponseDto(
                workspace.getId(),
                workspace.getOwnerId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.getVisibility()
        );
    }

    @PreAuthorize("@workspaceSecurity.isOwnerOrAdmin(#id)")
    @Override
    @Transactional
    public WorkspaceResponseDto updateWorkspace(UUID id, WorkspaceRequestDto requestDto) {

        WorkspaceEntity workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + id));

        workspace.setName(requestDto.getName());
        workspace.setDescription(requestDto.getDescription());
        workspace.setVisibility(requestDto.getVisibility());

        workspaceRepository.save(workspace);

        log.info("Successfully updated Workspace ID: {}", id);

        return new WorkspaceResponseDto(
                workspace.getId(),
                workspace.getOwnerId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.getVisibility()
        );
    }
}
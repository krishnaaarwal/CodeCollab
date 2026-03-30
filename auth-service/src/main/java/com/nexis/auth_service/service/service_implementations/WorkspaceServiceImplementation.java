package com.nexis.auth_service.service.service_implementations;

import com.nexis.auth_service.config.type.WorkspaceRole;
import com.nexis.auth_service.dto.workspace.WorkspaceMemberResponseDto;
import com.nexis.auth_service.dto.workspace.WorkspaceRequestDto;
import com.nexis.auth_service.dto.workspace.WorkspaceResponseDto;
import com.nexis.auth_service.entity.UserEntity;
import com.nexis.auth_service.entity.WorkspaceEntity;
import com.nexis.auth_service.entity.WorkspaceMemberEntity;
import com.nexis.auth_service.exception.DuplicateMemberException;
import com.nexis.auth_service.exception.ResourceNotFoundException;
import com.nexis.auth_service.repository.UserRepository;
import com.nexis.auth_service.repository.WorkspaceRepository;
import com.nexis.auth_service.security.authz.WorkspaceSecurity;
import com.nexis.auth_service.security.user_principal.UserPrincipal;
import com.nexis.auth_service.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
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
@Transactional
public class WorkspaceServiceImplementation implements WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceSecurity workspaceSecurity;

    @PreAuthorize("isAuthenticated()")
    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceResponseDto> getUserWorkspaces() {
        // 1. Get ONLY the ID from the Security Context
        Authentication authentication= SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = userPrincipal.getUserEntity().getId();

        // 2. Fetch a FRESH, Attached user using your current active Transaction
        UserEntity attachedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 3. Now you can safely call lazy lists!
        List<WorkspaceMemberEntity> workspaceMemberEntityList = attachedUser.getWorkspaceMemberList();
        log.info("Fetching workspaces for User ID: {}", attachedUser.getId());

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
    @Transactional(readOnly = true)
    public WorkspaceResponseDto getWorkspaceById(UUID id) {
       WorkspaceEntity workspace=  workspaceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        log.info("Fetching  Workspace with ID: {}", id);

        return new WorkspaceResponseDto(
                workspace.getId(),
                workspace.getOwnerId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.getVisibility()
        );
    }

    @PreAuthorize("isAuthenticated()")
    @Override
    @Transactional
    public void deleteUserFromWorkspace(UUID id, UUID memberId) {

        // 1. Get current logged-in user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID currentUserId = ((UserPrincipal) authentication.getPrincipal()).getUserEntity().getId();

        // 2. Get the Workspace
        WorkspaceEntity workspace =  workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        // 3. Find the specific "Badge" connecting this User to this Workspace
        WorkspaceMemberEntity memberToRemove = workspace.getMembers().stream()
                .filter(member -> member.getUser().getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this workspace"));

        //4. Find the badge connected to Current user
        WorkspaceMemberEntity currentUserBadge = workspace.getMembers().stream()
                .filter(member -> member.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this workspace"));

        //5. Primary security check
        boolean isSelfRemove = currentUserId.equals(memberId);
        boolean isKick = (currentUserBadge.getRole()== WorkspaceRole.OWNER || currentUserBadge.getRole()== WorkspaceRole.ADMIN);

        // If you aren't removing yourself, AND you aren't an Admin/Owner... Blocked!
        if (!isSelfRemove && !isKick) {
            throw new AccessDeniedException("You do not have permission to remove this member.");
        }

        if (memberToRemove.getRole() == WorkspaceRole.OWNER) {
            throw new IllegalArgumentException("Cannot remove the OWNER from the workspace.");
        }

        // 6. Owner cannot remove itself
        if (memberToRemove.getRole() == WorkspaceRole.OWNER) {
            throw new IllegalArgumentException("The OWNER cannot leave or be removed. Transfer ownership or delete the workspace first.");
        }

        workspace.getMembers().remove(memberToRemove);

        log.info("Successfully removed User ID: {} from Workspace ID: {}", memberId, id);
    }

    @PreAuthorize("isAuthenticated()")
    @Override
    @Transactional
    public WorkspaceResponseDto createWorkspace(WorkspaceRequestDto requestDto) {
        // 1. Get ONLY the ID from the Security Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = userPrincipal.getUserEntity().getId();

        // 2. Fetch a FRESH, Attached user
        UserEntity attachedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UUID ownerId = attachedUser.getId();

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
                .user(attachedUser)
                .role(WorkspaceRole.OWNER)
                .joinedAt(LocalDateTime.now())
                .build();

        workspace.getMembers().add(workspaceMember);

        // This will now work perfectly because attachedUser has an open database session!
        attachedUser.getWorkspaceMemberList().add(workspaceMember);

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

    @PreAuthorize("@workspaceSecurity.isOwnerOrAdmin(#workspaceId) or @workspaceSecurity.isMember(#workspaceId)")
    @Override
    @Transactional(readOnly = true) // Optimize for reading!
    public List<WorkspaceMemberResponseDto> getWorkspaceMembers(UUID workspaceId) {

        WorkspaceEntity workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + workspaceId));

        log.info("Fetching members for Workspace ID: {}", workspaceId);

        return workspace.getMembers().stream().map(
                member -> new WorkspaceMemberResponseDto(
                        member.getUser().getId(),
                        member.getUser().getFullname(),
                        member.getUser().getEmail(),
                        member.getRole()
                )
        ).toList();
    }


}
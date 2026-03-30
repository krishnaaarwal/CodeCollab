package com.nexis.auth_service.controller;

import com.nexis.auth_service.dto.workspace.WorkspaceMemberResponseDto;
import com.nexis.auth_service.dto.workspace.WorkspaceRequestDto;
import com.nexis.auth_service.dto.workspace.WorkspaceResponseDto;
import com.nexis.auth_service.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
/*
API Endpoints:
        • GET /api/workspaces - List user's workspaces
        * GET /api/workspaces/{id} - get single workspace
        • POST /api/workspaces - Create new workspace
        • PUT /api/workspaces/{id} - Update workspace
        • POST /api/workspaces/{id}/members - Add member
        * GET /api/workspaces/{id}/members - See all members in workspace
        * DELETE /api/workspaces/{id}/members/{memberId} - Delete user from workspace
 */



@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workspaces")
public class WorkspaceController {
    private final WorkspaceService workspaceService;

    @GetMapping()
    public ResponseEntity<List<WorkspaceResponseDto>> getUserWorkspaces(){
        log.info("Fetching all workspaces for the authenticated user");
        return ResponseEntity.status(HttpStatus.OK).body(workspaceService.getUserWorkspaces());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponseDto> getWorkspaceById(@PathVariable UUID id){
        log.info("Fetching workspace for the ID: {}",id);
        return ResponseEntity.status(HttpStatus.OK).body(workspaceService.getWorkspaceById(id));
    }

    @PostMapping()
    public ResponseEntity<WorkspaceResponseDto> createWorkspace(@RequestBody @Valid WorkspaceRequestDto requestDto){
        log.info("Received request to create new workspace: '{}'", requestDto.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(workspaceService.createWorkspace(requestDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceResponseDto> updateWorkspace(@PathVariable UUID id, @RequestBody @Valid WorkspaceRequestDto requestDto){
        log.info("Received request to update workspace ID: {}", id);
        return ResponseEntity.status(HttpStatus.OK).body(workspaceService.updateWorkspace(id, requestDto));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<WorkspaceResponseDto> addWorkspaceMember(@PathVariable UUID id, @RequestParam("memberId") UUID memberId){
        log.info("Received request to add user ID: {} to workspace ID: {}", memberId, id);
        return ResponseEntity.status(HttpStatus.CREATED).body(workspaceService.addWorkspaceMember(id, memberId));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<WorkspaceMemberResponseDto>> getWorkspaceMembers(@PathVariable UUID id){
        log.info("Received request to fetch members for workspace ID: {}", id);
        return ResponseEntity.ok(workspaceService.getWorkspaceMembers(id));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> deleteUserFromWorkspace(@PathVariable UUID id, @PathVariable UUID memberId){
        log.info("Received request to delete User ID: {} from Workspace ID: {}", memberId, id);
        workspaceService.deleteUserFromWorkspace(id, memberId);
        return ResponseEntity.noContent().build();
    }

}
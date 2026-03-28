package com.nexis.auth_service.controller;

import com.nexis.auth_service.dto.workspace.WorkspaceRequestDto;
import com.nexis.auth_service.dto.workspace.WorkspaceResponseDto;
import com.nexis.auth_service.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
API Endpoints:
        • GET /api/workspaces - List user's workspaces
        • POST /api/workspaces - Create new workspace
        • PUT /api/workspaces/{id} - Update workspace
        • POST /api/workspaces/{id}/members - Add member
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workspaces")
public class WorkspaceController {
    private final WorkspaceService workspaceService;

    @GetMapping()
    public ResponseEntity<List<WorkspaceResponseDto>> getUserWorkspaces(){
        return ResponseEntity.status(HttpStatus.OK).body(workspaceService.getUserWorkspaces());
    }

    @PostMapping()
    public ResponseEntity<WorkspaceResponseDto> createWorkspace(@RequestBody WorkspaceRequestDto requestDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(workspaceService.createWorkspace(requestDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceResponseDto> updateWorkspace(@PathVariable UUID id ,@RequestBody WorkspaceRequestDto requestDto){
        return ResponseEntity.status(HttpStatus.OK).body(workspaceService.updateWorkspace(id,requestDto));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<WorkspaceResponseDto> addWorkspace(@PathVariable UUID id ,@RequestParam("memberId") UUID memberId){
        return ResponseEntity.status(HttpStatus.CREATED).body(workspaceService.addWorkspaceMember(id,memberId));
    }
}

package com.nexis.storage_service.controller;

import com.nexis.storage_service.dto.FileRenameRequestDto;
import com.nexis.storage_service.dto.FileRequestDto;
import com.nexis.storage_service.dto.FileResponseDto;
import com.nexis.storage_service.dto.UploadCompleteDto;
import com.nexis.storage_service.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<FileResponseDto> uploadFile(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody FileRequestDto fileRequestDto) {

        UUID userUuid = UUID.fromString(userId);
        return ResponseEntity.ok(storageService.uploadFile(fileRequestDto,userUuid));
    }

    @PostMapping("/upload-complete")
    public ResponseEntity<Void> uploadComplete(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody UploadCompleteDto  uploadCompleteDto) {

        UUID userUuid = UUID.fromString(userId);
       storageService.uploadComplete(uploadCompleteDto,userUuid);
       return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<FileResponseDto> downloadFile(@RequestHeader("X-User-Id") String userId,
                                                    @PathVariable("id") UUID id) {
        UUID userUuid = UUID.fromString(userId);
        return ResponseEntity.ok(storageService.downloadFile(id,userUuid));
    }

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<FileResponseDto>> listWorkspaceFiles(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("workspaceId") UUID workspaceId) {

        UUID userUuid = UUID.fromString(userId);
        List<FileResponseDto> files = storageService.getFilesByWorkspace(workspaceId, userUuid);
        return ResponseEntity.ok(files);
    }
    @PatchMapping("/{fileId}/rename")
    public ResponseEntity<Void> renameFile(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID fileId,
            @RequestBody FileRenameRequestDto request) {

        storageService.renameFile(fileId, request, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID fileId,
            @RequestParam("workspaceId") UUID workspaceId) {

        storageService.deleteFile(fileId, workspaceId, userId);
        return ResponseEntity.noContent().build();
    }

}

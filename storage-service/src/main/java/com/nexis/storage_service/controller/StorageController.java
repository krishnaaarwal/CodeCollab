package com.nexis.storage_service.controller;

import com.nexis.storage_service.dto.FileRequestDto;
import com.nexis.storage_service.dto.FileResponseDto;
import com.nexis.storage_service.dto.UploadCompleteDto;
import com.nexis.storage_service.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

}

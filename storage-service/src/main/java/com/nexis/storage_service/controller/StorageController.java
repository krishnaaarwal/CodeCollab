package com.nexis.storage_service.controller;

import com.nexis.storage_service.dto.FileRequestDto;
import com.nexis.storage_service.dto.FileResponseDto;
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
    public ResponseEntity<FileResponseDto> upload(@RequestBody FileRequestDto fileRequestDto) {
        return ResponseEntity.ok(storageService.uploadFile(fileRequestDto));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<FileResponseDto> download(@RequestParam UUID id) {
        return ResponseEntity.ok(storageService.downloadFile(id));
    }

}

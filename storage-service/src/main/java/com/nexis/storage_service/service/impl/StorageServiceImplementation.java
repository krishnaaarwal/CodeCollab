package com.nexis.storage_service.service.impl;

import com.nexis.storage_service.dto.FileRequestDto;
import com.nexis.storage_service.dto.FileResponseDto;
import com.nexis.storage_service.entity.FileEntity;
import com.nexis.storage_service.entity.FileVersionEntity;
import com.nexis.storage_service.repository.FileRepository;
import com.nexis.storage_service.repository.FileVersionRepository;
import com.nexis.storage_service.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageServiceImplementation implements StorageService {

    private final FileRepository fileRepository;
    private final FileVersionRepository fileVersionRepository;

    @Override
    @Transactional
    public FileResponseDto uploadFile(FileRequestDto fileRequestDto,UUID userId) {
        List<FileEntity> listOfFiles = fileRepository.findByWorkspaceId(fileRequestDto.workspaceId());
        Optional<FileEntity> file = listOfFiles.stream()
                .filter(fileEntity -> fileEntity.getFileName().equals(fileRequestDto.fileName()))
                .findFirst();

        if(!file.isPresent()){

            //FILE ENTITY
            FileEntity fileEntity = FileEntity.builder().workspaceId(fileRequestDto.workspaceId())
                    .fileName(fileRequestDto.fileName()).fileSize(fileRequestDto.size())
                    .currentVersion(1).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            String[] fileNameOperationed = fileRequestDto.fileName().split(".");
            String fileType = fileNameOperationed[1];
            fileEntity.setFileType(fileType);

            String storageKey = "nexis/"+fileRequestDto.workspaceId()+"/"+fileEntity.getFileName()+"/"+fileEntity.getId()+"_v"+fileEntity.getCurrentVersion();
            fileEntity.setStorageKey(storageKey);

            fileRepository.save(fileEntity);

            //FILE VERSION ENTITY
            FileVersionEntity fileVersionEntity = FileVersionEntity.builder().fileId(fileEntity.getId())
                    .fileSize(fileEntity.getFileSize()).version(1).storageKey(storageKey)
                    .createdAt(LocalDateTime.now()).build();
            //created by will add later when user will be extracted from headers
        }

        FileEntity updatedFile = FileEntity.builder()
                //.fileName()   How can we handle renaming , user can rename file then all previous one should change
                .fileSize(fileRequestDto.size()).currentVersion(file.get().getCurrentVersion()+1).updatedAt(LocalDateTime.now()).build();

        String storageKey = "nexis/"+fileRequestDto.workspaceId()+"/"+updatedFile.getFileName()+"/"+updatedFile.getId()+"_v"+updatedFile.getCurrentVersion();
        updatedFile.setStorageKey(storageKey);

        fileRepository.save(updatedFile);

        //Created by implemented later
        FileVersionEntity newFileVersion = FileVersionEntity.builder().fileId(updatedFile.getId()).fileSize(fileRequestDto.size()).version(updatedFile.getCurrentVersion()).storageKey(storageKey).createdAt(LocalDateTime.now()).build();

        fileVersionRepository.save(newFileVersion);
        return null;
    }

    @Override
    @Transactional
    public FileResponseDto downloadFile(UUID id,UUID userId) {
        return null;
    }
}

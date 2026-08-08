package com.ashik.askaboutme.service;

import com.ashik.askaboutme.configdto.StorageProvider;
import com.ashik.askaboutme.dto.FileMetadataResponse;
import com.ashik.askaboutme.entity.FileUpload;
import com.ashik.askaboutme.entity.UploadStatus;
import com.ashik.askaboutme.repository.FileUploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileMetadataService {

    private static final int PREVIEW_URL_EXPIRY_SECONDS = (int) Duration.ofMinutes(15).toSeconds();

    private final FileUploadRepository fileUploadRepository;
    private final FileStorageResolver fileStorageResolver;

    @Transactional(readOnly = true)
    public List<FileMetadataResponse> listFiles() {
        return fileUploadRepository.findByIsDeletedFalseAndStatusOrderByCreatedAtDesc(UploadStatus.COMPLETED)
                .stream()
                .map(this::toMetadataResponse)
                .toList();
    }

    private FileMetadataResponse toMetadataResponse(FileUpload fileUpload) {
        return new FileMetadataResponse(
                fileUpload.getId(),
                fileUpload.getFileName(),
                fileUpload.getContentType(),
                fileUpload.getFileSize(),
                StorageProvider.valueOf(fileUpload.getStorageProvider()),
                fileUpload.getStatus(),
                fileUpload.getEmbeddedStatus(),
                fileUpload.getCreatedAt(),
                resolvePreviewUrl(fileUpload)
        );
    }

    private String resolvePreviewUrl(FileUpload fileUpload) {
        try {
            FileStorageService storageService =
                    fileStorageResolver.resolve(StorageProvider.valueOf(fileUpload.getStorageProvider()));
            return storageService.getPreviewUrl(fileUpload.getObjectKey(), PREVIEW_URL_EXPIRY_SECONDS);
        } catch (Exception e) {
            log.error("Failed to generate preview URL for file {}", fileUpload.getId(), e);
            return null;
        }
    }
}

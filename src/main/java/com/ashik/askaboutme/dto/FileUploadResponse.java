package com.ashik.askaboutme.dto;

import com.ashik.askaboutme.configdto.StorageProvider;
import com.ashik.askaboutme.entity.FileUpload;
import com.ashik.askaboutme.entity.UploadStatus;

public record FileUploadResponse(
        Long id,
        String objectKey,
        String fileName,
        String contentType,
        Long fileSize,
        StorageProvider storageProvider,
        UploadStatus status
) {
    public static FileUploadResponse makeFileUploadResponse(FileUpload fileUpload) {
        return new FileUploadResponse(
                fileUpload.getId(),
                fileUpload.getObjectKey(),
                fileUpload.getFileName(),
                fileUpload.getContentType(),
                fileUpload.getFileSize(),
                StorageProvider.valueOf(fileUpload.getStorageProvider()),
                fileUpload.getStatus()
        );
    }
}

package com.ashik.askaboutme.dto;

import com.ashik.askaboutme.configdto.StorageProvider;
import com.ashik.askaboutme.entity.FileUpload;
import com.ashik.askaboutme.entity.UploadStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record FileUploadResponse(
        @Schema(description = "Upload identifier", example = "1")
        Long id,

        @Schema(description = "Storage key the file is stored under", example = "3f1a9c2e-1234-4a56-9abc-1234567890ab-handbook.pdf")
        String objectKey,

        @Schema(description = "Original file name", example = "handbook.pdf")
        String fileName,

        @Schema(description = "MIME type", example = "application/pdf")
        String contentType,

        @Schema(description = "File size in bytes", example = "10485760")
        Long fileSize,

        @Schema(description = "Storage backend the file was uploaded to")
        StorageProvider storageProvider,

        @Schema(description = "Upload lifecycle status. A COMPLETED file becomes eligible for automatic embedding.")
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

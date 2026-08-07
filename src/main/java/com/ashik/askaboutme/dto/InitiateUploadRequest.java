package com.ashik.askaboutme.dto;

import com.ashik.askaboutme.configdto.StorageProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record InitiateUploadRequest(
        @NotBlank(message = "fileName is required") String fileName,
        String contentType,
        @Positive(message = "fileSize must be positive") Long fileSize,
        StorageProvider storageProvider
) {
}

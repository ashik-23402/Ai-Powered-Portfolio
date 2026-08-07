package com.ashik.askaboutme.dto;

public record InitiateUploadResponse(
        Long fileId,
        String objectKey,
        String uploadId
) {
}

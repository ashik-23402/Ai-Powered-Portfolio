package com.ashik.askaboutme.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record InitiateUploadResponse(
        @Schema(description = "Identifier for this upload; use it in subsequent /parts and /complete calls", example = "1")
        Long fileId,

        @Schema(description = "Storage key the file will be stored under", example = "3f1a9c2e-1234-4a56-9abc-1234567890ab-handbook.pdf")
        String objectKey,

        @Schema(description = "Multipart upload session ID issued by the storage provider; echo it back if the provider ever requires it explicitly", example = "MWExMDcxODktNGZjZi00OTVhLWE3YmYtMjMyM2RlNzhkNTA1")
        String uploadId
) {
}

package com.ashik.askaboutme.dto;

import com.ashik.askaboutme.configdto.StorageProvider;
import com.ashik.askaboutme.entity.EmbeddedStatus;
import com.ashik.askaboutme.entity.UploadStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record FileMetadataResponse(
        @Schema(description = "Upload identifier", example = "1")
        Long id,

        @Schema(description = "Original file name", example = "handbook.pdf")
        String fileName,

        @Schema(description = "MIME type", example = "application/pdf")
        String contentType,

        @Schema(description = "File size in bytes", example = "10485760")
        Long fileSize,

        @Schema(description = "Storage backend the file is stored on")
        StorageProvider storageProvider,

        @Schema(description = "Upload lifecycle status")
        UploadStatus status,

        @Schema(description = "Whether this file's content has been embedded into the vector store")
        EmbeddedStatus embeddedStatus,

        @Schema(description = "When the upload was created", example = "2026-08-07T13:39:37.069295825Z")
        Instant createdAt,

        @Schema(description = "Short-lived presigned URL for previewing/downloading the file directly "
                + "from storage (expires 15 minutes after this response). Null if the URL could not be generated.",
                example = "http://localhost:9000/askaboutme/3f1a9c2e-...-handbook.pdf?X-Amz-Signature=...")
        String previewUrl
) {
}

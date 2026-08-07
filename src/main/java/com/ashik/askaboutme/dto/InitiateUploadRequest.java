package com.ashik.askaboutme.dto;

import com.ashik.askaboutme.configdto.StorageProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record InitiateUploadRequest(
        @Schema(
                description = "Original file name, including extension. The extension determines how the "
                        + "file is parsed during embedding (pdf, docx, doc, md, txt are supported).",
                example = "handbook.pdf",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "fileName is required") String fileName,

        @Schema(description = "MIME type of the file, forwarded to the storage provider", example = "application/pdf")
        String contentType,

        @Schema(
                description = "Total file size in bytes",
                example = "10485760",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive(message = "fileSize must be positive") Long fileSize,

        @Schema(description = "Object storage backend to use. Defaults to MINIO if omitted.", example = "MINIO")
        StorageProvider storageProvider
) {
}

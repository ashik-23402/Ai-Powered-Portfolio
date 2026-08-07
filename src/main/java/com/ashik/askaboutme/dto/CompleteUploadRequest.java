package com.ashik.askaboutme.dto;

import com.ashik.askaboutme.configdto.UploadPart;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CompleteUploadRequest(
        @Schema(
                description = "All parts uploaded via POST /files/{fileId}/parts/{partNumber}, in any order",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "parts is required") List<UploadPart> parts
) {
}

package com.ashik.askaboutme.dto;

import com.ashik.askaboutme.entity.DeletionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record FileDeleteResponse(
        @Schema(description = "Deleted file's identifier", example = "1")
        Long fileId,

        @Schema(description = "Cleanup status. PROCESSING means storage/vector-store cleanup is "
                + "underway or was just retried; the file is already hidden either way.")
        DeletionStatus deletionStatus
) {
}

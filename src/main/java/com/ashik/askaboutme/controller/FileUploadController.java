package com.ashik.askaboutme.controller;

import com.ashik.askaboutme.configdto.UploadPart;
import com.ashik.askaboutme.dto.CompleteUploadRequest;
import com.ashik.askaboutme.dto.FileUploadResponse;
import com.ashik.askaboutme.dto.InitiateUploadRequest;
import com.ashik.askaboutme.dto.InitiateUploadResponse;
import com.ashik.askaboutme.exception.ErrorResponse;
import com.ashik.askaboutme.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "File Upload")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Operation(
            summary = "Start a new multipart upload",
            description = "Registers a new file upload and opens a multipart upload session with the "
                    + "configured storage provider (MinIO by default). Returns the fileId and uploadId "
                    + "needed for subsequent /parts and /complete calls."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload session created",
                    content = @Content(schema = @Schema(implementation = InitiateUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed (e.g. blank fileName, non-positive fileSize)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Storage provider failed to open the upload session",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/initiate")
    public InitiateUploadResponse initiateUpload(@Valid @RequestBody InitiateUploadRequest request) {
        return fileUploadService.initiateUpload(request);
    }

    @Operation(
            summary = "Upload one part of a file",
            description = "Uploads a single chunk of a multipart upload previously started via /initiate. "
                    + "Parts are numbered starting at 1. Returns the ETag needed to reference this part in /complete."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Part uploaded",
                    content = @Content(schema = @Schema(implementation = UploadPart.class))),
            @ApiResponse(responseCode = "404", description = "fileId does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Upload is not in a modifiable state (already completed/deleted), or the storage provider rejected the part",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "Part exceeds the maximum allowed upload size",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/{fileId}/parts/{partNumber}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadPart uploadPart(
            @Parameter(description = "ID returned by /initiate", example = "1") @PathVariable Long fileId,
            @Parameter(description = "1-based part number", example = "1") @PathVariable int partNumber,
            @Parameter(description = "Binary content of this part", required = true) @RequestParam("file") MultipartFile file) {
        return fileUploadService.uploadPart(fileId, partNumber, file);
    }

    @Operation(
            summary = "Complete a multipart upload",
            description = "Finalizes the upload once all parts have been uploaded, assembling them at the "
                    + "storage provider and marking the file COMPLETED. A COMPLETED, non-deleted file becomes "
                    + "eligible for automatic embedding on the next scheduler run (about once a minute)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload completed",
                    content = @Content(schema = @Schema(implementation = FileUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed (e.g. empty parts list)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "fileId does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Upload is not in a modifiable state, or the storage provider failed to assemble the parts",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{fileId}/complete")
    public FileUploadResponse completeUpload(
            @Parameter(description = "ID returned by /initiate", example = "1") @PathVariable Long fileId,
            @Valid @RequestBody CompleteUploadRequest request) {
        return fileUploadService.completeUpload(fileId, request);
    }
}

package com.ashik.askaboutme.controller;

import com.ashik.askaboutme.configdto.UploadPart;
import com.ashik.askaboutme.dto.CompleteUploadRequest;
import com.ashik.askaboutme.dto.FileUploadResponse;
import com.ashik.askaboutme.dto.InitiateUploadRequest;
import com.ashik.askaboutme.dto.InitiateUploadResponse;
import com.ashik.askaboutme.service.FileUploadService;
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

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping("/initiate")
    public InitiateUploadResponse initiateUpload(@Valid @RequestBody InitiateUploadRequest request) {
        return fileUploadService.initiateUpload(request);
    }

    @PostMapping(value = "/{fileId}/parts/{partNumber}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadPart uploadPart(@PathVariable Long fileId, @PathVariable int partNumber,
                                  @RequestParam("file") MultipartFile file) {
        return fileUploadService.uploadPart(fileId, partNumber, file);
    }

    @PostMapping("/{fileId}/complete")
    public FileUploadResponse completeUpload(@PathVariable Long fileId, @Valid @RequestBody CompleteUploadRequest request) {
        return fileUploadService.completeUpload(fileId, request);
    }
}

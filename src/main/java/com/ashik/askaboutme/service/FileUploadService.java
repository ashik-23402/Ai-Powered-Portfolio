package com.ashik.askaboutme.service;

import com.ashik.askaboutme.configdto.StorageProvider;
import com.ashik.askaboutme.configdto.UploadPart;
import com.ashik.askaboutme.dto.CompleteUploadRequest;
import com.ashik.askaboutme.dto.FileDeleteResponse;
import com.ashik.askaboutme.dto.FileUploadResponse;
import com.ashik.askaboutme.dto.InitiateUploadRequest;
import com.ashik.askaboutme.dto.InitiateUploadResponse;
import com.ashik.askaboutme.entity.EmbeddedStatus;
import com.ashik.askaboutme.entity.FileUpload;
import com.ashik.askaboutme.entity.UploadStatus;
import com.ashik.askaboutme.repository.FileUploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final FileStorageResolver fileStorageResolver;
    private final FileUploadRepository fileUploadRepository;
    private final FileUploadDeletionStatusService fileUploadDeletionStatusService;
    private final FileDeletionProcessingService fileDeletionProcessingService;

    @Transactional
    public InitiateUploadResponse initiateUpload(InitiateUploadRequest request) {
        StorageProvider provider = request.storageProvider() != null ? request.storageProvider() : StorageProvider.MINIO;
        FileStorageService storageService = fileStorageResolver.resolve(provider);

        String objectKey = UUID.randomUUID() + "-" + request.fileName();

        String uploadId;
        try {
            uploadId = storageService.initiateUpload(objectKey, request.contentType());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initiate upload for file " + request.fileName(), e);
        }

        Instant now = Instant.now();
        FileUpload fileUpload = new FileUpload();
        fileUpload.setObjectKey(objectKey);
        fileUpload.setFileName(request.fileName());
        fileUpload.setContentType(request.contentType());
        fileUpload.setFileSize(request.fileSize());
        fileUpload.setStorageProvider(provider.name());
        fileUpload.setStorageUploadId(uploadId);
        fileUpload.setStatus(UploadStatus.INITIATED);
        fileUpload.setCreatedAt(now);
        fileUpload.setUpdatedAt(now);
        fileUpload.setEmbeddedStatus(EmbeddedStatus.PENDING);

        fileUpload = fileUploadRepository.save(fileUpload);
        log.info("Initiated upload {} for file {} (objectKey={})", fileUpload.getId(), request.fileName(), objectKey);

        return new InitiateUploadResponse(fileUpload.getId(), objectKey, uploadId);
    }

    @Transactional
    public UploadPart uploadPart(Long fileId, int partNumber, MultipartFile file) {
        FileUpload fileUpload = getModifiableUpload(fileId);
        FileStorageService storageService = fileStorageResolver.resolve(StorageProvider.valueOf(fileUpload.getStorageProvider()));

        String etag;
        try (InputStream inputStream = file.getInputStream()) {
            etag = storageService.uploadPart(fileUpload.getObjectKey(), fileUpload.getStorageUploadId(), partNumber,
                    inputStream, file.getSize());
        } catch (Exception e) {
            markFailed(fileUpload);
            throw new IllegalStateException("Failed to upload part " + partNumber + " for file " + fileId, e);
        }

        fileUpload.setStatus(UploadStatus.UPLOADING);
        fileUpload.setUpdatedAt(Instant.now());
        fileUploadRepository.save(fileUpload);

        return new UploadPart(partNumber, etag);
    }

    @Transactional
    public FileUploadResponse completeUpload(Long fileId, CompleteUploadRequest request) {
        FileUpload fileUpload = getModifiableUpload(fileId);
        FileStorageService storageService = fileStorageResolver.resolve(StorageProvider.valueOf(fileUpload.getStorageProvider()));

        try {
            storageService.completeUpload(fileUpload.getObjectKey(), fileUpload.getStorageUploadId(), request.parts());
        } catch (Exception e) {
            markFailed(fileUpload);
            throw new IllegalStateException("Failed to complete upload for file " + fileId, e);
        }

        fileUpload.setStatus(UploadStatus.COMPLETED);
        fileUpload.setUpdatedAt(Instant.now());
        fileUpload = fileUploadRepository.save(fileUpload);
        log.info("Completed upload {} (objectKey={})", fileUpload.getId(), fileUpload.getObjectKey());

        return FileUploadResponse.makeFileUploadResponse(fileUpload);
    }

    public FileDeleteResponse deleteFile(Long fileId) {
        FileUpload fileUpload = fileUploadDeletionStatusService.requestDeletion(fileId);
        fileDeletionProcessingService.process(fileUpload);
        return new FileDeleteResponse(fileUpload.getId(), fileUpload.getDeletionStatus());
    }

    private FileUpload getModifiableUpload(Long fileId) {
        FileUpload fileUpload = fileUploadRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File upload not found: " + fileId));
        if (fileUpload.isDeleted() || fileUpload.getStatus() == UploadStatus.COMPLETED) {
            throw new IllegalStateException("File upload " + fileId + " is not in a modifiable state");
        }
        return fileUpload;
    }

    private void markFailed(FileUpload fileUpload) {
        fileUpload.setStatus(UploadStatus.FAILED);
        fileUpload.setUpdatedAt(Instant.now());
        fileUploadRepository.save(fileUpload);
    }
}

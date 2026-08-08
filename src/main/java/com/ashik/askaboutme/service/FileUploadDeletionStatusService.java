package com.ashik.askaboutme.service;

import com.ashik.askaboutme.entity.DeletionStatus;
import com.ashik.askaboutme.entity.FileUpload;
import com.ashik.askaboutme.repository.FileUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileUploadDeletionStatusService {

    private final FileUploadRepository fileUploadRepository;

    @Transactional
    public FileUpload requestDeletion(Long fileId) {
        FileUpload fileUpload = fileUploadRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File upload not found: " + fileId));
        if (fileUpload.isDeleted()) {
            throw new IllegalStateException("File upload " + fileId + " is already deleted");
        }

        fileUpload.setDeleted(true);
        fileUpload.setDeletionStatus(DeletionStatus.PROCESSING);
        fileUpload.setUpdatedAt(Instant.now());
        return fileUploadRepository.save(fileUpload);
    }

    @Transactional
    public List<FileUpload> claimFailedDeletions() {
        List<FileUpload> candidates = fileUploadRepository
                .findTop5ByIsDeletedTrueAndDeletionStatusOrderByUpdatedAtAsc(DeletionStatus.FAILED);
        if (candidates.isEmpty()) {
            return candidates;
        }

        Instant now = Instant.now();
        candidates.forEach(fileUpload -> {
            fileUpload.setDeletionStatus(DeletionStatus.PROCESSING);
            fileUpload.setUpdatedAt(now);
        });
        return fileUploadRepository.saveAll(candidates);
    }

    @Transactional
    public void updateDeletionStatus(Long fileId, DeletionStatus status) {
        fileUploadRepository.findById(fileId).ifPresent(fileUpload -> {
            fileUpload.setDeletionStatus(status);
            fileUpload.setUpdatedAt(Instant.now());
            fileUploadRepository.save(fileUpload);
        });
    }

    public void deleteFromDeletionStatus(Long fileId) {
        fileUploadRepository.deleteById(fileId);
    }
}

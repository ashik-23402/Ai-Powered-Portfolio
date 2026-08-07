package com.ashik.askaboutme.service;

import com.ashik.askaboutme.entity.EmbeddedStatus;
import com.ashik.askaboutme.entity.FileUpload;
import com.ashik.askaboutme.entity.UploadStatus;
import com.ashik.askaboutme.repository.FileUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Owns every transactional mutation of {@link FileUpload#getEmbeddedStatus()}. Kept as a
 * standalone bean (rather than methods on the scheduler/async services) so {@code @Transactional}
 * always goes through the Spring proxy instead of being silently skipped by same-class self-invocation.
 */
@Service
@RequiredArgsConstructor
public class FileUploadEmbeddingStatusService {

    private final FileUploadRepository fileUploadRepository;

    /**
     * Fetches the oldest pending uploads and flips them to PROCESSING in the same transaction so
     * they are committed before any async embedding work starts - this is what keeps a second
     * scheduler tick (or another cluster node, absent ShedLock) from picking up the same rows.
     */
    @Transactional
    public List<FileUpload> claimPendingUploads() {
        List<FileUpload> candidates = fileUploadRepository
                .findTop5ByEmbeddedStatusAndStatusOrderByCreatedAtAsc(EmbeddedStatus.PENDING, UploadStatus.COMPLETED);
        if (candidates.isEmpty()) {
            return candidates;
        }

        Instant now = Instant.now();
        candidates.forEach(fileUpload -> {
            fileUpload.setEmbeddedStatus(EmbeddedStatus.PROCESSING);
            fileUpload.setUpdatedAt(now);
        });
        return fileUploadRepository.saveAll(candidates);
    }

    @Transactional
    public void updateEmbeddedStatus(Long fileUploadId, EmbeddedStatus status) {
        fileUploadRepository.findById(fileUploadId).ifPresent(fileUpload -> {
            fileUpload.setEmbeddedStatus(status);
            fileUpload.setUpdatedAt(Instant.now());
            fileUploadRepository.save(fileUpload);
        });
    }
}

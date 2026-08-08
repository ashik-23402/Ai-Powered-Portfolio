package com.ashik.askaboutme.service;

import com.ashik.askaboutme.configdto.StorageProvider;
import com.ashik.askaboutme.entity.DeletionStatus;
import com.ashik.askaboutme.entity.FileUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Removes a soft-deleted {@link FileUpload}'s data from the vector store and object storage. Runs
 * off the request thread via {@code @Async} so the delete endpoint returns immediately.
 * <p>
 * The two cleanup steps are independent and each fault-isolated: a failure in one does not stop the
 * other from being attempted, and either failing marks the file FAILED rather than throwing, so the
 * {@link FileDeletionSchedulerService} retries just the steps that matter (both operations are
 * idempotent - deleting an already-gone object or an empty filter match is a no-op) on its next tick.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileDeletionProcessingService {

    private final FileStorageResolver fileStorageResolver;
    private final VectorStore vectorStore;
    private final FileUploadDeletionStatusService fileUploadDeletionStatusService;

    @Async
    public void process(FileUpload fileUpload) {
        boolean vectorStoreCleaned = deleteVectorStoreChunks(fileUpload);
        boolean objectRemoved = deleteStorageObject(fileUpload);

        if (vectorStoreCleaned && objectRemoved) {
            fileUploadDeletionStatusService.updateDeletionStatus(fileUpload.getId(), DeletionStatus.COMPLETED);
            log.info("Deletion completed for file {}", fileUpload.getId());
        } else {
            fileUploadDeletionStatusService.updateDeletionStatus(fileUpload.getId(), DeletionStatus.FAILED);
            log.warn("Deletion incomplete for file {} (vectorStoreCleaned={}, objectRemoved={}); will retry",
                    fileUpload.getId(), vectorStoreCleaned, objectRemoved);
        }
    }

    private boolean deleteVectorStoreChunks(FileUpload fileUpload) {
        try {
            Filter.Expression filter = new FilterExpressionBuilder()
                    .eq("fileUploadId", fileUpload.getId())
                    .build();
            vectorStore.delete(filter);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete vector store chunks for file {}", fileUpload.getId(), e);
            return false;
        }
    }

    private boolean deleteStorageObject(FileUpload fileUpload) {
        try {
            FileStorageService storageService =
                    fileStorageResolver.resolve(StorageProvider.valueOf(fileUpload.getStorageProvider()));
            storageService.removeObject(fileUpload.getObjectKey());
            return true;
        } catch (Exception e) {
            log.error("Failed to remove storage object for file {}", fileUpload.getId(), e);
            return false;
        }
    }
}

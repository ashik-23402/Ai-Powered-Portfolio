package com.ashik.askaboutme.service;

import com.ashik.askaboutme.entity.FileUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileDeletionSchedulerService {

    private final FileUploadDeletionStatusService fileUploadDeletionStatusService;
    private final FileDeletionProcessingService fileDeletionProcessingService;

    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "fileDeletionRetryLock", lockAtLeastFor = "PT10S", lockAtMostFor = "PT50S")
    public void retryFailedDeletions() {
        List<FileUpload> claimed = fileUploadDeletionStatusService.claimFailedDeletions();
        if (claimed.isEmpty()) {
            return;
        }

        log.info("Retrying {} failed file deletion(s)", claimed.size());
        claimed.forEach(fileDeletionProcessingService::process);
    }
}

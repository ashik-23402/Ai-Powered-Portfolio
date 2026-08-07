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
public class EmbeddingSchedulerService {

    private final FileUploadEmbeddingStatusService fileUploadEmbeddingStatusService;
    private final EmbeddingProcessingService embeddingProcessingService;

    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "embeddingPipelineLock", lockAtLeastFor = "PT10S", lockAtMostFor = "PT50S")
    public void runEmbeddingPipeline() {
        log.info("Starting embedding processing");
        List<FileUpload> claimed = fileUploadEmbeddingStatusService.claimPendingUploads();
        if (claimed.isEmpty()) {
            return;
        }

        log.info("Claimed {} file upload(s) for embedding", claimed.size());
        claimed.forEach(embeddingProcessingService::process);
    }
}

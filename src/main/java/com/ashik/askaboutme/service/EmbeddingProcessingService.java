package com.ashik.askaboutme.service;

import com.ashik.askaboutme.configdto.StorageProvider;
import com.ashik.askaboutme.entity.EmbeddedStatus;
import com.ashik.askaboutme.entity.FileUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Turns a claimed {@link FileUpload} into embeddings. Runs off the request thread via
 * {@code @Async} (bounded pool, see spring.task.execution.pool.*) so the ShedLock-guarded claim
 * step stays fast.
 * <p>
 * Memory strategy for large files: the source object is streamed from MinIO straight to a temp
 * file on disk (never buffered as a byte[]), the document readers work off that disk-backed
 * Resource, and the PDF reader emits one Document per page instead of one huge string for the
 * whole file. Each page/document is split and flushed to the vector store immediately rather
 * than accumulating a whole-file chunk list first, so we never hold more than one page's worth
 * of split chunks in memory at a time; PgVectorStore additionally batches its embedding calls to
 * Ollama within that per-page add() according to spring.ai.vectorstore.pgvector.max-document-batch-size.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingProcessingService {

    private final FileStorageResolver fileStorageResolver;
    private final DocumentExtractor documentExtractor;
    private final VectorStore vectorStore;
    private final FileUploadEmbeddingStatusService fileUploadEmbeddingStatusService;

    private final TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder().build();

    @Async
    public void process(FileUpload fileUpload) {
        Path tempFile = null;
        try {
            tempFile = downloadToTempFile(fileUpload);

            List<Document> pages = documentExtractor.extract(new FileSystemResource(tempFile), fileUpload.getFileName());

            int chunkCount = 0;
            for (Document page : pages) {
                enrichMetadata(page, fileUpload);
                List<Document> pageChunks = tokenTextSplitter.split(page);
                if (!pageChunks.isEmpty()) {
                    vectorStore.add(pageChunks);
                    chunkCount += pageChunks.size();
                }
            }

            fileUploadEmbeddingStatusService.updateEmbeddedStatus(fileUpload.getId(), EmbeddedStatus.COMPLETED);
            log.info("Embedding completed for file {} ({} page(s), {} chunk(s))",
                    fileUpload.getId(), pages.size(), chunkCount);
        } catch (Exception e) {
            log.error("Embedding failed for file {}", fileUpload.getId(), e);
            fileUploadEmbeddingStatusService.updateEmbeddedStatus(fileUpload.getId(), EmbeddedStatus.PENDING);
        } finally {
            deleteQuietly(tempFile);
        }
    }

    private Path downloadToTempFile(FileUpload fileUpload) throws Exception {
        FileStorageService storageService =
                fileStorageResolver.resolve(StorageProvider.valueOf(fileUpload.getStorageProvider()));

        Path tempFile = Files.createTempFile("embed-" + fileUpload.getId() + "-", suffixOf(fileUpload.getFileName()));
        try (InputStream inputStream = storageService.downloadObject(fileUpload.getObjectKey())) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
        return tempFile;
    }

    private void enrichMetadata(Document document, FileUpload fileUpload) {
        document.getMetadata().put("fileUploadId", fileUpload.getId());
        document.getMetadata().put("fileName", fileUpload.getFileName());
        document.getMetadata().put("objectKey", fileUpload.getObjectKey());
    }

    private String suffixOf(String fileName) {
        int dotIndex = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(dotIndex) : ".tmp";
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("Failed to delete temp file {}", path, e);
        }
    }
}

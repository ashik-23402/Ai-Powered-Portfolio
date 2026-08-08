package com.ashik.askaboutme.repository;

import com.ashik.askaboutme.entity.DeletionStatus;
import com.ashik.askaboutme.entity.EmbeddedStatus;
import com.ashik.askaboutme.entity.FileUpload;
import com.ashik.askaboutme.entity.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {

    List<FileUpload> findTop5ByEmbeddedStatusAndStatusOrderByCreatedAtAsc(
            EmbeddedStatus embeddedStatus,
            UploadStatus status
    );

    List<FileUpload> findTop5ByIsDeletedTrueAndDeletionStatusOrderByUpdatedAtAsc(
            DeletionStatus deletionStatus
    );

    List<FileUpload> findByIsDeletedFalseAndStatusOrderByCreatedAtDesc(
            UploadStatus status
    );
}

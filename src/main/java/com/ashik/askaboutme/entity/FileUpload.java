package com.ashik.askaboutme.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity(name = "file_upload")
@Data
public class FileUpload {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String objectKey;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private String storageProvider;

    private String storageUploadId;

    @Enumerated(EnumType.STRING)
    private UploadStatus status;

    private Instant createdAt;

    private Instant updatedAt;

    private boolean isDeleted;

    @Enumerated(EnumType.STRING)
    private EmbeddedStatus embeddedStatus;

    @Enumerated(EnumType.STRING)
    private DeletionStatus deletionStatus;
}

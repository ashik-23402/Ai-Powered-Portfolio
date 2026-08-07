package com.ashik.askaboutme.repository;

import com.ashik.askaboutme.entity.FileUpload;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {
}

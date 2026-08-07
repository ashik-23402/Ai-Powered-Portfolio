package com.ashik.askaboutme.service;

import com.ashik.askaboutme.configdto.StorageProvider;
import com.ashik.askaboutme.configdto.UploadPart;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.XmlParserException;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface FileStorageService {
    String initiateUpload(String objectKey , String contentType)
            throws InsufficientDataException, IOException, NoSuchAlgorithmException, InvalidKeyException, XmlParserException, InternalException;
    String uploadPart(String objectKey, String uploadId, int partNumber, InputStream inputStream, long size)
            throws InsufficientDataException, IOException, NoSuchAlgorithmException, InvalidKeyException, XmlParserException, InternalException;
    void completeUpload(String objectKey, String uploadId, List<UploadPart> parts)
            throws InsufficientDataException, IOException, NoSuchAlgorithmException, InvalidKeyException, XmlParserException, InternalException;
    StorageProvider provider();
}

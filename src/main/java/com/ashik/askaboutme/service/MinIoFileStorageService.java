package com.ashik.askaboutme.service;

import com.ashik.askaboutme.configdto.MinIoConfigProperties;
import com.ashik.askaboutme.configdto.StorageProvider;
import com.ashik.askaboutme.configdto.UploadPart;
import com.ashik.askaboutme.utils.CompletableFutureBlock;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import io.minio.*;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.XmlParserException;
import io.minio.messages.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties({MinIoConfigProperties.class})
public class MinIoFileStorageService implements FileStorageService {

    private final MinIoConfigProperties minIoConfigProperties;
    private final MinioAsyncClient minioAsyncClient;

    @Override
    public String initiateUpload(String objectKey, String contentType)
            throws InsufficientDataException, IOException, NoSuchAlgorithmException,
            InvalidKeyException, XmlParserException, InternalException {
        log.info("Initiating Minio upload for object key {}", objectKey);
        ensureBucketExists(minIoConfigProperties.bucketName());
        HashMultimap<String, String> headers = HashMultimap.create();
        if(null != contentType) {
            headers.put("Content-Type", contentType);
        }
        var multipartUploadResponse = CompletableFutureBlock.block(minioAsyncClient.createMultipartUploadAsync(
                minIoConfigProperties.bucketName(), null, objectKey, headers, ImmutableMultimap.of()));
        log.info("Created Minio upload for object key {} {}", objectKey , multipartUploadResponse);

        return multipartUploadResponse.result().uploadId();
    }

    @Override
    public String uploadPart(String objectKey, String uploadId, int partNumber, InputStream inputStream, long size)
            throws InsufficientDataException, IOException, NoSuchAlgorithmException, InvalidKeyException,
            XmlParserException, InternalException {
        log.info("Uploading part {} for object key {} with uploadId {}", partNumber, objectKey, uploadId);
        var response = CompletableFutureBlock.block(minioAsyncClient.uploadPartAsync(
                minIoConfigProperties.bucketName(), null, objectKey, inputStream, size, uploadId,
                partNumber, ImmutableMultimap.of(), ImmutableMultimap.of()));
        log.info("Uploaded part {} for object key {} with uploadId {} {}", partNumber, objectKey, uploadId, response);

        return response.etag();
    }

    @Override
    public void completeUpload(String objectKey, String uploadId, List<UploadPart> parts) throws InsufficientDataException, IOException, NoSuchAlgorithmException, InvalidKeyException, XmlParserException, InternalException {
        log.info("Completing Minio upload for object key {} with uploadId {}", objectKey, uploadId);
        Part[] minioParts = parts.stream().map(part -> new Part(part.partNumber(), part.etag())).toArray(Part[]::new);
        CompletableFutureBlock.block(minioAsyncClient.completeMultipartUploadAsync(minIoConfigProperties.bucketName(),
                null, objectKey, uploadId, minioParts, ImmutableMultimap.of(), ImmutableMultimap.of()));

    }

    @Override
    public StorageProvider provider() {
        return StorageProvider.MINIO;
    }

    private void ensureBucketExists(String bucketName)
            throws InsufficientDataException,
            IOException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            XmlParserException,
            InternalException {
        boolean exist = CompletableFutureBlock.block(minioAsyncClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()));
        log.info("Bucket {} exists {}", bucketName, exist);
        if (!exist) {
            log.info("Bucket {} does not exist", bucketName);
            CompletableFutureBlock.block(minioAsyncClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()));
            log.info("Bucket {} created", bucketName);
        }
    }
}

package com.ashik.askaboutme.configdto;


import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "minio")
@Validated
public record MinIoConfigProperties(
        @NotBlank(message = "minio.url is required")
        String url,
        @NotBlank(message = "minio.access-key is required")
        String accessKey,
        @NotBlank(message = "minio.secret-key is required")
        String secretKey,
        @NotBlank(message = "minio.bucket-name is required")
        String bucketName
) {
}

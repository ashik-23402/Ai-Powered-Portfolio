package com.ashik.askaboutme.config;

import com.ashik.askaboutme.configdto.MinIoConfigProperties;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(MinIoConfigProperties.class)
@RequiredArgsConstructor
public class MinIoConfig {
    private final MinIoConfigProperties minIoConfigProperties;

    @Bean
    public MinioAsyncClient getMinioAsyncClient() {
        log.info("Initializing Minio async client");
        return MinioAsyncClient.builder()
                .endpoint(minIoConfigProperties.url())
                .credentials(minIoConfigProperties.accessKey(), minIoConfigProperties.secretKey())
                .build();
    }
}

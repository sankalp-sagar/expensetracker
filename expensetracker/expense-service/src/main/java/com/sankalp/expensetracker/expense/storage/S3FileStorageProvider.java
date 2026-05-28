package com.sankalp.expensetracker.expense.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * Activated when app.storage.provider=s3. Reads credentials from the standard AWS default chain
 * (env vars AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY/AWS_REGION, instance profile, etc.).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3FileStorageProvider implements FileStorageProvider {

    @Value("${app.storage.s3.bucket}")
    private String bucket;

    @Value("${app.storage.s3.region:us-east-1}")
    private String regionName;

    @Value("${app.storage.s3.presigned-ttl-seconds:600}")
    private long presignedTtlSeconds;

    private S3Client s3;
    private S3Presigner presigner;

    @PostConstruct
    public void init() {
        Region region = Region.of(regionName);
        s3 = S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        presigner = S3Presigner.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        log.info("S3 storage initialised: bucket={} region={}", bucket, regionName);
    }

    @PreDestroy
    public void close() {
        if (s3 != null) s3.close();
        if (presigner != null) presigner.close();
    }

    @Override
    public String store(MultipartFile file, String subPath) throws IOException {
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String safe = original.replaceAll("[^A-Za-z0-9._-]", "_");
        String key = subPath + "/" + UUID.randomUUID() + "_" + safe;
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket).key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        return key;
    }

    @Override
    public String resolveUrl(String storageKey) {
        var req = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignedTtlSeconds))
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(storageKey).build())
                .build();
        return presigner.presignGetObject(req).url().toString();
    }

    @Override
    public void delete(String storageKey) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storageKey).build());
    }
}

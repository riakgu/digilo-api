package com.riakgu.digilo.common.service;

import com.riakgu.digilo.common.exception.BadRequestException;
import com.riakgu.digilo.config.R2Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;

@Slf4j
@Service
public class StorageService {

    private final S3Client s3Client;
    private final R2Properties r2Properties;

    public StorageService(R2Properties r2Properties) {
        this.r2Properties = r2Properties;

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                r2Properties.getAccessKeyId(),
                r2Properties.getSecretAccessKey()
        );

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create("https://" + r2Properties.getAccountId() + ".r2.cloudflarestorage.com"))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .chunkedEncodingEnabled(false)
                                .build()
                )
                .build();
    }


    public String upload(MultipartFile file, String key) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        try {
            byte[] bytes = file.getBytes();
            
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(r2Properties.getBucketName())
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength((long) bytes.length)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(bytes));
            log.info("File uploaded: {}", key);

            return buildPublicUrl(key);

        } catch (IOException e) {
            log.error("Upload failed for key {}: {}", key, e.getMessage());
            throw new BadRequestException("Failed to upload file");
        }
    }

    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(r2Properties.getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.info("File deleted: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete file {}: {}", key, e.getMessage());
        }
    }

    public String extractKey(String url) {
        if (url != null && url.startsWith(getPublicUrlBase())) {
            return url.substring(getPublicUrlBase().length() + 1);
        }
        return null;
    }

    public String buildPublicUrl(String key) {
        return getPublicUrlBase() + "/" + key;
    }

    public String getPublicUrlBase() {
        String base = r2Properties.getPublicUrl();
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

}

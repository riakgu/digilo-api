package com.riakgu.digilo.common.service;

import com.riakgu.digilo.config.R2Properties;
import com.riakgu.digilo.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Slf4j
@Service
public class R2ImageService {

    private final S3Client s3Client;
    private final R2Properties r2Properties;

    public R2ImageService(R2Properties r2Properties) {
        this.r2Properties = r2Properties;
        
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            r2Properties.getAccessKeyId(),
            r2Properties.getSecretAccessKey()
        );

        this.s3Client = S3Client.builder()
            .endpointOverride(URI.create("https://" + r2Properties.getAccountId() + ".r2.cloudflarestorage.com"))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of("auto"))
            .build();
    }

    public String uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        try {
            String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2Properties.getBucketName())
                .key(fileName)
                .contentType(file.getContentType())
                .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            return r2Properties.getPublicUrl() + "/" + fileName;

        } catch (IOException e) {
            log.error("Error uploading to R2", e);
            throw new BadRequestException("Failed to upload image");
        }
    }

    public void deleteImage(String fileName) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(r2Properties.getBucketName())
                .key(fileName)
                .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            log.error("Error deleting from R2", e);
        }
    }

    public String extractFileNameIfR2(String imageUrl) {
        if (imageUrl != null && imageUrl.startsWith(r2Properties.getPublicUrl())) {
            return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        }
        return null;
    }
}

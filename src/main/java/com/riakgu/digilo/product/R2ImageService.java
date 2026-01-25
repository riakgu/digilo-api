package com.riakgu.digilo.product;

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
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
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
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .chunkedEncodingEnabled(false)
                                .build()
                )
                .build();
    }

    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed");
        }

        String extension = extensionFromContentType(contentType);
        if (extension == null) {
            throw new BadRequestException("Unsupported image type");
        }

        String fileName = UUID.randomUUID() + "." + extension;
        Path tempFile = null;

        try {
            tempFile = Files.createTempFile("upload-", "." + extension);
            file.transferTo(tempFile.toFile());

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(r2Properties.getBucketName())
                    .key(fileName)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromFile(tempFile));

            return buildPublicUrl(fileName);

        } catch (IOException e) {
            log.error("Upload failed", e);
            throw new BadRequestException("Failed to upload image");
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temp file: {}", tempFile);
                }
            }
        }
    }

    public void deleteImage(String fileName) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(r2Properties.getBucketName())
                    .key(fileName)
                    .build();

            s3Client.deleteObject(request);

        } catch (Exception e) {
            log.error("Failed to delete image from R2: {}", fileName, e);
        }
    }

    public String extractFileNameIfR2(String imageUrl) {
        if (imageUrl != null && imageUrl.startsWith(r2Properties.getPublicUrl())) {
            return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        }
        return null;
    }

    private String buildPublicUrl(String fileName) {
        String base = r2Properties.getPublicUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + fileName;
    }

    private String extensionFromContentType(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> null;
        };
    }
}

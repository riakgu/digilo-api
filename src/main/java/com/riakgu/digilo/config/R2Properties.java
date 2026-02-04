package com.riakgu.digilo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudflare.r2")
public record R2Properties(
        String accountId,
        String accessKeyId,
        String secretAccessKey,
        String bucketName,
        String publicUrl
) {}

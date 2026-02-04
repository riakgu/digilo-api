package com.riakgu.digilo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.encryption")
public record EncryptionProperties(
        String password,
        String salt
) {}

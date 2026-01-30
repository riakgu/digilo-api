package com.riakgu.digilo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "waha")
public record WahaProperties(
        String baseUrl,
        String apiKey,
        String session
) {}

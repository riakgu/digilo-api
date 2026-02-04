package com.riakgu.digilo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "midtrans")
public record MidtransProperties(
        String serverKey,
        String clientKey,
        String baseUrl,
        boolean production
) {}
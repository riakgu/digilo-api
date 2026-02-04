package com.riakgu.digilo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String accessSecret,
        long accessExpiration,
        String refreshSecret,
        long refreshExpiration,
        String issuer
) {}

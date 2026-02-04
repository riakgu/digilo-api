package com.riakgu.digilo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        int defaultLimit,           // requests per window for unauthenticated users
        int authenticatedLimit,     // requests per window for authenticated users
        int authEndpointLimit,      // requests per window for auth endpoints (login/register)
        int windowSeconds           // time window in seconds
) {}

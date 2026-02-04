package com.riakgu.digilo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.site")
public record SiteProperties(
        String name,
        String companyName,
        String frontendUrl,
        String supportUrl
) {}

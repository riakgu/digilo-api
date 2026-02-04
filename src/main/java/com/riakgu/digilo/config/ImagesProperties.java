package com.riakgu.digilo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.images")
public record ImagesProperties(
        String productPlaceholder
) {}

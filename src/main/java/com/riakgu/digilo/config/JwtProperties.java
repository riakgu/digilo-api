package com.riakgu.digilo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String accessSecret;

    private long accessExpiration;

    private String refreshSecret;

    private long refreshExpiration;

    private String issuer;

}

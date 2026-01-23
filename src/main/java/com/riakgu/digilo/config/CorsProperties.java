package com.riakgu.digilo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Setter
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    @Getter
    private List<String> allowedOrigins = List.of("http://localhost:3000");
    
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    
    @Getter
    private List<String> allowedHeaders = List.of("*");
    
    @Getter
    private List<String> exposedHeaders = List.of("Authorization");
    
    @Getter
    private boolean allowCredentials = true;
    
    @Getter
    private long maxAge = 3600;

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    // Support comma-separated string from environment variable
    public void setAllowedOrigins(String origins) {
        if (origins != null && !origins.isBlank()) {
            this.allowedOrigins = Arrays.asList(origins.split(","));
        }
    }

    // Support list from YAML
    public void setAllowedOrigins(List<String> origins) {
        this.allowedOrigins = origins;
    }
}


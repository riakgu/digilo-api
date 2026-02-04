package com.riakgu.digilo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.site")
@Data
public class SiteProperties {

    private String name = "Digilo";

    private String companyName = "Digilo";

    private String frontendUrl = "http://localhost:3000";

    private String supportUrl = "http://localhost:3000/support";
}

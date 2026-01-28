package com.riakgu.digilo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.images")
@Data
public class ImagesProperties {

    private String productPlaceholder;

}

package com.riakgu.digilo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "midtrans")
@Data
public class MidtransProperties {

    private String serverKey;

    private String clientKey;

    private String baseUrl = "https://api.sandbox.midtrans.com";

    private boolean production = false;

}
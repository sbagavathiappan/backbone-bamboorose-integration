package com.backbonebamboorose.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "webhook")
public class WebhookProperties {

    private Security security = new Security();
    private Backbone backbone = new Backbone();
    private Bamboorose bamboorose = new Bamboorose();

    @Data
    public static class Security {
        private String secret;
        private String headerName = "X-Webhook-Signature";
    }

    @Data
    public static class Backbone {
        private String baseUrl;
        private String apiKey;
    }

    @Data
    public static class Bamboorose {
        private String baseUrl;
        private String apiKey;
        private String apiSecret;
    }
}

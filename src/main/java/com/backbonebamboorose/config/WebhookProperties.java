package com.backbonebamboorose.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "webhook")
public class WebhookProperties {

    private Security security = new Security();
    private Bkbn bkbn = new Bkbn();
    private Bamboorose bamboorose = new Bamboorose();

    @Data
    public static class Security {
        private String webhookSecret;
    }

    @Data
    public static class Bkbn {
        private String baseUrl = "https://sync.bkbn.com";
        private String clientId;
        private String clientSecret;
        private String jwtToken;
    }

    @Data
    public static class Bamboorose {
        private String baseUrl = "https://api.bamboorose.com";
        private String apiKey;
        private String apiSecret;
    }
}

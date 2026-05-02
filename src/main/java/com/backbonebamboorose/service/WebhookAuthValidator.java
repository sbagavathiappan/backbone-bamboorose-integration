package com.backbonebamboorose.service;

import com.backbonebamboorose.config.WebhookProperties;
import com.backbonebamboorose.exception.WebhookAuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookAuthValidator {

    private final WebhookProperties webhookProperties;

    public void validateAuth(String authHeader) {
        String expectedSecret = webhookProperties.getSecurity().getWebhookSecret();
        if (expectedSecret == null || expectedSecret.isEmpty()) {
            log.warn("No webhook secret configured; accepting unauthenticated requests");
            return;
        }

        if (authHeader == null || authHeader.isBlank()) {
            throw new WebhookAuthException("Missing authentication header");
        }

        String token;
        if (authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        } else {
            token = authHeader.trim();
        }

        if (!constantTimeEquals(token, expectedSecret)) {
            log.warn("Webhook authentication failed: invalid token");
            throw new WebhookAuthException("Invalid authentication token");
        }

        log.debug("Webhook authentication validated successfully");
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}

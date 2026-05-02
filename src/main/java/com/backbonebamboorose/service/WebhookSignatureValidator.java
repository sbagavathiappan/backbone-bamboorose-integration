package com.backbonebamboorose.service;

import com.backbonebamboorose.config.WebhookProperties;
import com.backbonebamboorose.exception.WebhookSignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookSignatureValidator {

    private final WebhookProperties webhookProperties;
    private static final String HMAC_SHA256 = "HmacSHA256";

    public void validateSignature(String payload, String signature) {
        if (signature == null || signature.isEmpty()) {
            throw new WebhookSignatureException("Missing webhook signature");
        }

        String expectedSignature = computeSignature(payload);

        if (!constantTimeEquals(expectedSignature, signature)) {
            log.warn("Webhook signature validation failed. Expected signature does not match.");
            throw new WebhookSignatureException("Invalid webhook signature");
        }

        log.debug("Webhook signature validated successfully");
    }

    private String computeSignature(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookProperties.getSecurity().getSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Failed to compute webhook signature", e);
            throw new WebhookSignatureException("Failed to compute signature");
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}

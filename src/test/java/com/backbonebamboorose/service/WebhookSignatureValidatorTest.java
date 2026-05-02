package com.backbonebamboorose.service;

import com.backbonebamboorose.config.WebhookProperties;
import com.backbonebamboorose.exception.WebhookSignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookSignatureValidatorTest {

    @Mock
    private WebhookProperties webhookProperties;

    @Mock
    private WebhookProperties.Security securityProperties;

    private WebhookSignatureValidator validator;

    private static final String TEST_SECRET = "test-secret-key";

    @BeforeEach
    void setUp() {
        when(webhookProperties.getSecurity()).thenReturn(securityProperties);
        when(securityProperties.getSecret()).thenReturn(TEST_SECRET);
        validator = new WebhookSignatureValidator(webhookProperties);
    }

    @Test
    void validateSignature_shouldAcceptValidSignature() throws Exception {
        String payload = "{\"quote_id\":\"Q-001\"}";
        String expectedSignature = computeHmacSha256(payload, TEST_SECRET);

        assertDoesNotThrow(() -> validator.validateSignature(payload, expectedSignature));
    }

    @Test
    void validateSignature_shouldRejectInvalidSignature() {
        String payload = "{\"quote_id\":\"Q-001\"}";
        String invalidSignature = "invalid-signature";

        assertThrows(WebhookSignatureException.class,
                () -> validator.validateSignature(payload, invalidSignature));
    }

    @Test
    void validateSignature_shouldRejectNullSignature() {
        String payload = "{\"quote_id\":\"Q-001\"}";

        assertThrows(WebhookSignatureException.class,
                () -> validator.validateSignature(payload, null));
    }

    @Test
    void validateSignature_shouldRejectEmptySignature() {
        String payload = "{\"quote_id\":\"Q-001\"}";

        assertThrows(WebhookSignatureException.class,
                () -> validator.validateSignature(payload, ""));
    }

    @Test
    void validateSignature_shouldRejectModifiedPayload() throws Exception {
        String originalPayload = "{\"quote_id\":\"Q-001\"}";
        String modifiedPayload = "{\"quote_id\":\"Q-002\"}";
        String signature = computeHmacSha256(originalPayload, TEST_SECRET);

        assertThrows(WebhookSignatureException.class,
                () -> validator.validateSignature(modifiedPayload, signature));
    }

    private String computeHmacSha256(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}

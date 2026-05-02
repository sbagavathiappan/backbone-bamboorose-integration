package com.backbonebamboorose.service;

import com.backbonebamboorose.config.WebhookProperties;
import com.backbonebamboorose.exception.WebhookAuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookAuthValidatorTest {

    @Mock
    private WebhookProperties webhookProperties;

    @Mock
    private WebhookProperties.Security securityProperties;

    private WebhookAuthValidator validator;

    private static final String TEST_SECRET = "test-secret-token";

    @BeforeEach
    void setUp() {
        when(webhookProperties.getSecurity()).thenReturn(securityProperties);
        when(securityProperties.getWebhookSecret()).thenReturn(TEST_SECRET);
        validator = new WebhookAuthValidator(webhookProperties);
    }

    @Test
    void validateAuth_shouldAcceptValidBearerToken() {
        assertDoesNotThrow(() -> validator.validateAuth("Bearer " + TEST_SECRET));
    }

    @Test
    void validateAuth_shouldAcceptValidPlainToken() {
        assertDoesNotThrow(() -> validator.validateAuth(TEST_SECRET));
    }

    @Test
    void validateAuth_shouldRejectInvalidToken() {
        assertThrows(WebhookAuthException.class,
                () -> validator.validateAuth("Bearer invalid-token"));
    }

    @Test
    void validateAuth_shouldRejectNullToken() {
        assertThrows(WebhookAuthException.class,
                () -> validator.validateAuth(null));
    }

    @Test
    void validateAuth_shouldRejectBlankToken() {
        assertThrows(WebhookAuthException.class,
                () -> validator.validateAuth("   "));
    }

    @Test
    void validateAuth_shouldAcceptAllWhenNoSecretConfigured() {
        when(securityProperties.getWebhookSecret()).thenReturn(null);
        assertDoesNotThrow(() -> validator.validateAuth("any-token"));
    }
}

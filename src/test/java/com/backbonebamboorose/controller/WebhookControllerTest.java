package com.backbonebamboorose.controller;

import com.backbonebamboorose.config.WebhookProperties;
import com.backbonebamboorose.dto.WebhookResponse;
import com.backbonebamboorose.model.WebhookEvent;
import com.backbonebamboorose.service.QuoteSyncService;
import com.backbonebamboorose.service.WebhookReceiverService;
import com.backbonebamboorose.service.WebhookSignatureValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WebhookReceiverService webhookReceiverService;

    @MockBean
    private QuoteSyncService quoteSyncService;

    @MockBean
    private WebhookSignatureValidator webhookSignatureValidator;

    @MockBean
    private WebhookProperties webhookProperties;

    @MockBean
    private WebhookProperties.Security securityProperties;

    private static final String TEST_SECRET = "test-secret";

    @BeforeEach
    void setUp() {
        when(webhookProperties.getSecurity()).thenReturn(securityProperties);
        when(securityProperties.getSecret()).thenReturn(TEST_SECRET);
    }

    @Test
    void handleBackboneQuoteWebhook_shouldReturnAccepted() throws Exception {
        String payload = "{\"id\":\"Q-001\",\"quote_number\":\"QT-2024-001\",\"status\":\"CREATED\"}";
        String signature = computeSignature(payload);

        WebhookEvent mockEvent = WebhookEvent.builder()
                .eventId("event-123")
                .quoteId("Q-001")
                .status(WebhookEvent.EventStatus.PENDING)
                .build();

        when(webhookReceiverService.receiveWebhook(any(), any(), any())).thenReturn(mockEvent);
        when(quoteSyncService.processWebhookEvent(any())).thenReturn(CompletableFuture.completedFuture(null));

        mockMvc.perform(post("/webhook/backbone/quotes")
                        .header("X-Webhook-Signature", signature)
                        .header("X-Webhook-Event-Type", "QUOTE_CREATED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.event_id").value("event-123"))
                .andExpect(jsonPath("$.quote_id").value("Q-001"));
    }

    @Test
    void handleBackboneQuoteWebhook_shouldReturnBadRequestOnSignatureFailure() throws Exception {
        String payload = "{\"id\":\"Q-001\"}";

        mockMvc.perform(post("/webhook/backbone/quotes")
                        .header("X-Webhook-Signature", "invalid-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleQuoteSyncRequest_shouldReturnAccepted() throws Exception {
        String payload = "{\"id\":\"Q-001\",\"quote_number\":\"QT-2024-001\"}";
        String signature = computeSignature(payload);

        WebhookEvent mockEvent = WebhookEvent.builder()
                .eventId("event-456")
                .quoteId("Q-001")
                .status(WebhookEvent.EventStatus.PENDING)
                .build();

        when(webhookReceiverService.receiveWebhook(any(), any(), any())).thenReturn(mockEvent);
        when(quoteSyncService.processWebhookEvent(any())).thenReturn(CompletableFuture.completedFuture(null));

        mockMvc.perform(post("/webhook/backbone/quotes/sync")
                        .header("X-Webhook-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true));
    }

    private String computeSignature(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
                TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}

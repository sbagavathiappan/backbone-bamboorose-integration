package com.backbonebamboorose.controller;

import com.backbonebamboorose.dto.WebhookResponse;
import com.backbonebamboorose.model.WebhookEvent;
import com.backbonebamboorose.service.QuoteSyncService;
import com.backbonebamboorose.service.WebhookReceiverService;
import com.backbonebamboorose.service.WebhookAuthValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
    private WebhookAuthValidator webhookAuthValidator;

    @Test
    void handleBkbnWebhook_shouldReturnOk() throws Exception {
        String payload = """
                {
                    "event": "VISUALS_READY",
                    "orderId": "ORD-12345",
                    "assignmentId": "ASM-001",
                    "visualType": "POST",
                    "product": "GROUND_PHOTO",
                    "timestamp": "2024-01-15T10:30:00Z"
                }
                """;

        WebhookEvent mockEvent = WebhookEvent.builder()
                .eventId("event-123")
                .orderId("ORD-12345")
                .assignmentId("ASM-001")
                .status(WebhookEvent.EventStatus.PENDING)
                .build();

        when(webhookReceiverService.receiveWebhook(any(), any(), any())).thenReturn(mockEvent);
        when(quoteSyncService.processWebhookEvent(any())).thenReturn(CompletableFuture.completedFuture(null));

        mockMvc.perform(post("/webhook/bkbn/visuals")
                        .header("Authorization", "Bearer test-token")
                        .header("X-Event-Type", "VISUALS_READY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.event_id").value("event-123"))
                .andExpect(jsonPath("$.order_id").value("ORD-12345"));
    }

    @Test
    void handleBkbnWebhook_shouldReturnBadRequestOnAuthFailure() throws Exception {
        String payload = "{\"event\": \"VISUALS_READY\", \"orderId\": \"ORD-12345\", \"assignmentId\": \"ASM-001\"}";

        mockMvc.perform(post("/webhook/bkbn/visuals")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}

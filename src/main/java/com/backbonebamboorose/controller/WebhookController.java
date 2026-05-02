package com.backbonebamboorose.controller;

import com.backbonebamboorose.dto.WebhookResponse;
import com.backbonebamboorose.model.WebhookEvent;
import com.backbonebamboorose.service.QuoteSyncService;
import com.backbonebamboorose.service.WebhookReceiverService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Validated
public class WebhookController {

    private final WebhookReceiverService webhookReceiverService;
    private final QuoteSyncService quoteSyncService;

    @PostMapping("/backbone/quotes")
    public ResponseEntity<WebhookResponse> handleBackboneQuoteWebhook(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestHeader(value = "X-Webhook-Event-Type", defaultValue = "QUOTE_CREATED") String eventType,
            @RequestBody @NotBlank String payload) {

        log.info("Received Backbone quote webhook: eventType={}", eventType);

        try {
            WebhookEvent event = webhookReceiverService.receiveWebhook(eventType, payload, signature);

            quoteSyncService.processWebhookEvent(event);

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(WebhookResponse.success(
                            event.getEventId(),
                            event.getQuoteId(),
                            "Webhook received and processing started"
                    ));

        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(WebhookResponse.failure(null, "Failed to process webhook: " + e.getMessage()));
        }
    }

    @PostMapping("/backbone/quotes/sync")
    public ResponseEntity<WebhookResponse> handleQuoteSyncRequest(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestBody @NotBlank String payload) {

        log.info("Received quote sync request");

        return handleBackboneQuoteWebhook(signature, "QUOTE_SYNC_REQUEST", payload);
    }
}

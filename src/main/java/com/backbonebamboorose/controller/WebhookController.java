package com.backbonebamboorose.controller;

import com.backbonebamboorose.dto.WebhookResponse;
import com.backbonebamboorose.model.WebhookEvent;
import com.backbonebamboorose.service.QuoteSyncService;
import com.backbonebamboorose.service.WebhookReceiverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Webhooks", description = "Endpoints for receiving Backbone PLM quote webhooks")
public class WebhookController {

    private final WebhookReceiverService webhookReceiverService;
    private final QuoteSyncService quoteSyncService;

    @PostMapping("/backbone/quotes")
    @Operation(summary = "Receive a Backbone PLM quote webhook", description = "Validates signature, persists the event, and triggers async quote synchronization to Bamboo Rose")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Webhook accepted and processing started"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or signature")
    })
    public ResponseEntity<WebhookResponse> handleBackboneQuoteWebhook(
            @Parameter(description = "HMAC-SHA256 signature of the payload")
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @Parameter(description = "Event type: QUOTE_CREATED, QUOTE_UPDATED, QUOTE_APPROVED, QUOTE_REJECTED, QUOTE_EXPIRED, QUOTE_SYNC_REQUEST")
            @RequestHeader(value = "X-Webhook-Event-Type", defaultValue = "QUOTE_CREATED") String eventType,
            @Parameter(description = "JSON payload of the Backbone PLM quote", required = true)
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
    @Operation(summary = "Trigger a manual quote sync", description = "Same as the standard webhook endpoint but defaults to QUOTE_SYNC_REQUEST event type")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Sync request accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or signature")
    })
    public ResponseEntity<WebhookResponse> handleQuoteSyncRequest(
            @Parameter(description = "HMAC-SHA256 signature of the payload")
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @Parameter(description = "JSON payload of the Backbone PLM quote", required = true)
            @RequestBody @NotBlank String payload) {

        log.info("Received quote sync request");

        return handleBackboneQuoteWebhook(signature, "QUOTE_SYNC_REQUEST", payload);
    }
}

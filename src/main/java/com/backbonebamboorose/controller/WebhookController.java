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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Endpoints for receiving BKBN VISUALS_READY webhooks")
public class WebhookController {

    private final WebhookReceiverService webhookReceiverService;
    private final QuoteSyncService quoteSyncService;

    @PostMapping("/bkbn/visuals")
    @Operation(
            summary = "Receive a BKBN VISUALS_READY webhook",
            description = "Validates auth header, persists the event, fetches materials from BKBN, and triggers async sync to Bamboo Rose. Responds 200 quickly per BKBN spec."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or missing required fields"),
            @ApiResponse(responseCode = "401", description = "Authentication failed")
    })
    public ResponseEntity<WebhookResponse> handleBkbnWebhook(
            @Parameter(description = "Authorization header (Bearer token or API key)")
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "Event type header from BKBN (e.g. VISUALS_READY)")
            @RequestHeader(value = "X-Event-Type", required = false) String eventTypeHeader,
            @Parameter(description = "BKBN webhook payload JSON", required = true)
            @RequestBody @NotBlank String payload) {

        log.info("Received BKBN webhook: eventType={}", eventTypeHeader);

        try {
            WebhookEvent event = webhookReceiverService.receiveWebhook(authorization, eventTypeHeader, payload);

            quoteSyncService.processWebhookEvent(event);

            return ResponseEntity.ok(WebhookResponse.success(
                    event.getEventId(),
                    event.getOrderId(),
                    "Webhook processed successfully"
            ));

        } catch (Exception e) {
            log.error("Error processing BKBN webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(WebhookResponse.failure(null, "Failed to process webhook: " + e.getMessage()));
        }
    }
}

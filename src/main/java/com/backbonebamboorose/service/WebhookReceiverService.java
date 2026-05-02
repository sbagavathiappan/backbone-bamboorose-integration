package com.backbonebamboorose.service;

import com.backbonebamboorose.model.WebhookEvent;
import com.backbonebamboorose.repository.WebhookEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookReceiverService {

    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;
    private final WebhookSignatureValidator signatureValidator;

    @Transactional
    public WebhookEvent receiveWebhook(String eventType, String payload, String signature) {
        log.info("Receiving webhook event: type={}", eventType);

        signatureValidator.validateSignature(payload, signature);

        WebhookEvent.EventStatus status = WebhookEvent.EventStatus.PENDING;
        String quoteId = extractQuoteId(payload);

        WebhookEvent event = WebhookEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(WebhookEvent.EventType.valueOf(eventType))
                .source("BACKBONE_PLM")
                .quoteId(quoteId)
                .status(status)
                .payload(payload)
                .retryCount(0)
                .maxRetries(3)
                .build();

        WebhookEvent savedEvent = webhookEventRepository.save(event);
        log.info("Webhook event persisted: eventId={}, quoteId={}, status={}",
                savedEvent.getEventId(), savedEvent.getQuoteId(), savedEvent.getStatus());

        return savedEvent;
    }

    public WebhookEvent validateAndParseWebhook(String eventType, String payload, String signature) {
        signatureValidator.validateSignature(payload, signature);

        try {
            Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
            log.debug("Parsed webhook payload: keys={}", payloadMap.keySet());
            return null;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse webhook payload", e);
            throw new IllegalArgumentException("Invalid webhook payload format", e);
        }
    }

    private String extractQuoteId(String payload) {
        try {
            Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
            if (payloadMap.containsKey("data")) {
                Object data = payloadMap.get("data");
                if (data instanceof Map<?, ?> dataMap) {
                    if (dataMap.containsKey("id")) {
                        return String.valueOf(dataMap.get("id"));
                    }
                    if (dataMap.containsKey("quote_id")) {
                        return String.valueOf(dataMap.get("quote_id"));
                    }
                }
            }
            if (payloadMap.containsKey("quote_id")) {
                return String.valueOf(payloadMap.get("quote_id"));
            }
            if (payloadMap.containsKey("id")) {
                return String.valueOf(payloadMap.get("id"));
            }
        } catch (JsonProcessingException e) {
            log.warn("Could not extract quote_id from payload", e);
        }
        return null;
    }
}

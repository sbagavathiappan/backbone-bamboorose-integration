package com.backbonebamboorose.service;

import com.backbonebamboorose.exception.WebhookAuthException;
import com.backbonebamboorose.model.WebhookEvent;
import com.backbonebamboorose.model.bkbn.BkbnWebhookEvent;
import com.backbonebamboorose.repository.WebhookEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookReceiverService {

    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;
    private final WebhookAuthValidator authValidator;

    @Transactional
    public WebhookEvent receiveWebhook(String authHeader, String eventTypeHeader, String payload) {
        log.info("Receiving BKBN webhook: eventType={}", eventTypeHeader);

        authValidator.validateAuth(authHeader);

        BkbnWebhookEvent bkbnEvent = parsePayload(payload);

        String eventId = UUID.randomUUID().toString();

        WebhookEvent event = WebhookEvent.builder()
                .eventId(eventId)
                .eventType(bkbnEvent.getEvent())
                .source("BKBN")
                .orderId(bkbnEvent.getOrderId())
                .assignmentId(bkbnEvent.getAssignmentId())
                .status(WebhookEvent.EventStatus.PENDING)
                .payload(payload)
                .retryCount(0)
                .maxRetries(3)
                .materialsFetched(false)
                .build();

        WebhookEvent savedEvent = webhookEventRepository.save(event);
        log.info("Webhook event persisted: eventId={}, orderId={}, assignmentId={}, status={}",
                savedEvent.getEventId(), savedEvent.getOrderId(), savedEvent.getAssignmentId(), savedEvent.getStatus());

        return savedEvent;
    }

    private BkbnWebhookEvent parsePayload(String payload) {
        try {
            BkbnWebhookEvent event = objectMapper.readValue(payload, BkbnWebhookEvent.class);
            if (event.getEvent() == null || event.getOrderId() == null || event.getAssignmentId() == null) {
                throw new WebhookAuthException("Invalid BKBN webhook payload: missing required fields (event, orderId, assignmentId)");
            }
            return event;
        } catch (WebhookAuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse BKBN webhook payload", e);
            throw new WebhookAuthException("Invalid webhook payload format");
        }
    }
}

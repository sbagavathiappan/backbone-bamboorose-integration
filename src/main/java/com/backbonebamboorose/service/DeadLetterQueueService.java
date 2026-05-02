package com.backbonebamboorose.service;

import com.backbonebamboorose.model.WebhookEvent;
import com.backbonebamboorose.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueService {

    private final WebhookEventRepository webhookEventRepository;
    private final QuoteSyncService quoteSyncService;

    public List<WebhookEvent> getDeadLetterEvents() {
        log.debug("Retrieving dead letter queue events");
        return webhookEventRepository.findByStatus(WebhookEvent.EventStatus.FAILED);
    }

    @Transactional
    public void retryEvent(String eventId) {
        WebhookEvent event = webhookEventRepository.findByEventId(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        if (event.getStatus() != WebhookEvent.EventStatus.FAILED) {
            throw new IllegalArgumentException("Event is not in FAILED state, current status: " + event.getStatus());
        }

        log.info("Retrying dead letter event: eventId={}, previousFailures={}", eventId, event.getErrorMessage());

        event.setStatus(WebhookEvent.EventStatus.PENDING);
        event.setRetryCount(0);
        event.setErrorMessage(null);
        event.setNextRetryAt(null);
        event.setUpdatedAt(OffsetDateTime.now());
        webhookEventRepository.save(event);

        quoteSyncService.processWebhookEvent(event);
    }

    @Transactional
    public int retryAllEvents() {
        List<WebhookEvent> failedEvents = getDeadLetterEvents();
        log.info("Retrying all dead letter events: count={}", failedEvents.size());

        int count = 0;
        for (WebhookEvent event : failedEvents) {
            try {
                retryEvent(event.getEventId());
                count++;
            } catch (Exception e) {
                log.error("Failed to queue event for retry: eventId={}", event.getEventId(), e);
            }
        }

        return count;
    }

    @Transactional
    public void discardEvent(String eventId) {
        WebhookEvent event = webhookEventRepository.findByEventId(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        if (event.getStatus() != WebhookEvent.EventStatus.FAILED) {
            throw new IllegalArgumentException("Can only discard events in FAILED state");
        }

        log.warn("Discarding dead letter event: eventId={}", eventId);
        webhookEventRepository.delete(event);
    }
}

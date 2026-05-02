package com.backbonebamboorose.service;

import com.backbonebamboorose.model.WebhookEvent;
import com.backbonebamboorose.model.bkbn.BkbnMaterialsResponse;
import com.backbonebamboorose.model.bkbn.BkbnWebhookEvent;
import com.backbonebamboorose.model.bamboorose.BambooRoseQuote;
import com.backbonebamboorose.repository.WebhookEventRepository;
import com.backbonebamboorose.transformer.QuoteTransformer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteSyncService {

    private final WebhookEventRepository webhookEventRepository;
    private final BkbnClientService bkbnClientService;
    private final BambooRoseClientService bambooRoseClientService;
    private final QuoteTransformer quoteTransformer;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    private static final String SYNC_COUNTER = "webhook.sync.total";
    private static final String SUCCESS_COUNTER = "webhook.sync.success";
    private static final String FAILURE_COUNTER = "webhook.sync.failure";
    private static final String SYNC_TIMER = "webhook.sync.duration";

    @Async("webhookTaskExecutor")
    public CompletableFuture<Void> processWebhookEvent(WebhookEvent event) {
        log.info("Processing webhook event: eventId={}, type={}, orderId={}",
                event.getEventId(), event.getEventType(), event.getOrderId());

        Timer.Sample sample = Timer.start(meterRegistry);
        Counter.builder(SYNC_COUNTER)
                .tag("event_type", event.getEventType())
                .register(meterRegistry).increment();

        try {
            updateEventStatus(event, WebhookEvent.EventStatus.PROCESSING);

            BkbnWebhookEvent bkbnEvent = objectMapper.readValue(event.getPayload(), BkbnWebhookEvent.class);

            if (!Boolean.TRUE.equals(event.getMaterialsFetched())) {
                BkbnMaterialsResponse materials = bkbnClientService.getMaterials(
                        bkbnEvent.getOrderId(),
                        bkbnEvent.getAssignmentId(),
                        bkbnEvent.getProduct(),
                        bkbnEvent.getVisualType()
                );

                event.setMaterialsFetched(true);
                webhookEventRepository.save(event);
                log.info("Materials fetched for order={}: {} materials",
                        bkbnEvent.getOrderId(),
                        materials.getMaterials() != null ? materials.getMaterials().size() : 0);

                BambooRoseQuote bambooRoseQuote = quoteTransformer.transform(bkbnEvent, materials);

                bambooRoseClientService.syncQuote(bambooRoseQuote);
            } else {
                log.info("Materials already fetched, reprocessing event: eventId={}", event.getEventId());
                BkbnWebhookEvent existingEvent = objectMapper.readValue(event.getPayload(), BkbnWebhookEvent.class);
                BkbnMaterialsResponse materials = bkbnClientService.getMaterials(
                        existingEvent.getOrderId(),
                        existingEvent.getAssignmentId(),
                        existingEvent.getProduct(),
                        existingEvent.getVisualType()
                );
                BambooRoseQuote bambooRoseQuote = quoteTransformer.transform(existingEvent, materials);
                bambooRoseClientService.syncQuote(bambooRoseQuote);
            }

            updateEventStatus(event, WebhookEvent.EventStatus.COMPLETED);
            sample.stop(Timer.builder(SYNC_TIMER)
                    .tag("event_type", event.getEventType())
                    .tag("status", "success")
                    .register(meterRegistry));

            Counter.builder(SUCCESS_COUNTER)
                    .tag("event_type", event.getEventType())
                    .register(meterRegistry).increment();

            log.info("Webhook event processed successfully: eventId={}", event.getEventId());

        } catch (Exception e) {
            log.error("Failed to process webhook event: eventId={}", event.getEventId(), e);
            handleProcessingFailure(event, e);
            sample.stop(Timer.builder(SYNC_TIMER)
                    .tag("event_type", event.getEventType())
                    .tag("status", "failure")
                    .register(meterRegistry));

            Counter.builder(FAILURE_COUNTER)
                    .tag("event_type", event.getEventType())
                    .tag("error_type", e.getClass().getSimpleName())
                    .register(meterRegistry).increment();
        }

        return CompletableFuture.completedFuture(null);
    }

    @Scheduled(fixedDelayString = "${webhook.retry.fixed-delay:60000}",
               initialDelayString = "${webhook.retry.initial-delay:30000}")
    @Transactional
    public void processRetriableEvents() {
        log.debug("Checking for retriable webhook events");

        List<WebhookEvent> retriableEvents = webhookEventRepository.findRetriableEvents(OffsetDateTime.now());

        for (WebhookEvent event : retriableEvents) {
            if (event.getRetryCount() < event.getMaxRetries()) {
                log.info("Retrying webhook event: eventId={}, attempt={}/{}",
                        event.getEventId(), event.getRetryCount() + 1, event.getMaxRetries());

                event.setRetryCount(event.getRetryCount() + 1);
                event.setStatus(WebhookEvent.EventStatus.RETRYING);
                event.setNextRetryAt(calculateNextRetry(event.getRetryCount()));
                webhookEventRepository.save(event);

                processWebhookEvent(event);
            } else {
                log.warn("Max retries exceeded for webhook event: eventId={}", event.getEventId());
                event.setStatus(WebhookEvent.EventStatus.FAILED);
                event.setErrorMessage("Max retries (" + event.getMaxRetries() + ") exceeded");
                webhookEventRepository.save(event);
            }
        }
    }

    private void updateEventStatus(WebhookEvent event, WebhookEvent.EventStatus status) {
        event.setStatus(status);
        if (status == WebhookEvent.EventStatus.COMPLETED) {
            event.setCompletedAt(OffsetDateTime.now());
        }
        webhookEventRepository.save(event);
    }

    private void handleProcessingFailure(WebhookEvent event, Exception e) {
        event.setErrorMessage(e.getMessage());

        if (event.getRetryCount() < event.getMaxRetries()) {
            event.setStatus(WebhookEvent.EventStatus.RETRYING);
            event.setRetryCount(event.getRetryCount() + 1);
            event.setNextRetryAt(calculateNextRetry(event.getRetryCount()));
            log.info("Scheduled retry for event: eventId={}, attempt={}",
                    event.getEventId(), event.getRetryCount());
        } else {
            event.setStatus(WebhookEvent.EventStatus.FAILED);
            log.error("Event permanently failed: eventId={}", event.getEventId());
        }

        webhookEventRepository.save(event);
    }

    private OffsetDateTime calculateNextRetry(int retryCount) {
        long delaySeconds = (long) Math.pow(2, retryCount) * 10;
        return OffsetDateTime.now().plusSeconds(delaySeconds);
    }
}

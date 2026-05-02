package com.backbonebamboorose.controller;

import com.backbonebamboorose.model.WebhookEvent;
import com.backbonebamboorose.repository.WebhookEventRepository;
import com.backbonebamboorose.service.BkbnClientService;
import com.backbonebamboorose.service.BambooRoseClientService;
import com.backbonebamboorose.service.DeadLetterQueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Tag(name = "Management", description = "Internal endpoints for monitoring and managing webhook events")
public class InternalManagementController {

    private final WebhookEventRepository webhookEventRepository;
    private final BkbnClientService bkbnClientService;
    private final BambooRoseClientService bambooRoseClientService;
    private final DeadLetterQueueService deadLetterQueueService;

    @GetMapping("/events")
    @Operation(summary = "List all webhook events", description = "Returns paginated list of all webhook events")
    public ResponseEntity<Map<String, Object>> getEvents(
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        Page<WebhookEvent> events = webhookEventRepository.findAll(PageRequest.of(page, size));

        Map<String, Object> response = new HashMap<>();
        response.put("events", events.getContent());
        response.put("totalPages", events.getTotalPages());
        response.put("totalElements", events.getTotalElements());
        response.put("currentPage", events.getNumber());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/events/status/{status}")
    @Operation(summary = "Get events by status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of events"),
            @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    public ResponseEntity<List<WebhookEvent>> getEventsByStatus(
            @Parameter(description = "Event status: PENDING, PROCESSING, COMPLETED, FAILED, RETRYING")
            @PathVariable String status) {
        WebhookEvent.EventStatus eventStatus = WebhookEvent.EventStatus.valueOf(status.toUpperCase());
        List<WebhookEvent> events = webhookEventRepository.findByStatus(eventStatus);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/events/{eventId}")
    @Operation(summary = "Get event by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event found"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<WebhookEvent> getEventByEventId(@PathVariable String eventId) {
        return webhookEventRepository.findByEventId(eventId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/events/order/{orderId}")
    @Operation(summary = "Get events by order ID")
    public ResponseEntity<List<WebhookEvent>> getEventsByOrderId(@PathVariable String orderId) {
        List<WebhookEvent> events = webhookEventRepository.findByOrderId(orderId);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/events/type/{eventType}")
    @Operation(summary = "Get events by event type")
    public ResponseEntity<List<WebhookEvent>> getEventsByEventType(@PathVariable String eventType) {
        List<WebhookEvent> events = webhookEventRepository.findByEventType(eventType);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get event statistics")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", webhookEventRepository.countByStatus(WebhookEvent.EventStatus.PENDING));
        stats.put("processing", webhookEventRepository.countByStatus(WebhookEvent.EventStatus.PROCESSING));
        stats.put("completed", webhookEventRepository.countByStatus(WebhookEvent.EventStatus.COMPLETED));
        stats.put("failed", webhookEventRepository.countByStatus(WebhookEvent.EventStatus.FAILED));
        stats.put("retrying", webhookEventRepository.countByStatus(WebhookEvent.EventStatus.RETRYING));
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/health/external")
    @Operation(summary = "Check external API health")
    public ResponseEntity<Map<String, Object>> checkExternalHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("bkbn", bkbnClientService.healthCheck() ? "UP" : "DOWN");
        health.put("bambooRose", bambooRoseClientService.healthCheck() ? "UP" : "DOWN");
        health.put("timestamp", java.time.OffsetDateTime.now().toString());
        return ResponseEntity.ok(health);
    }

    @GetMapping("/events/recent")
    @Operation(summary = "Get recent events")
    public ResponseEntity<List<WebhookEvent>> getRecentEvents() {
        List<WebhookEvent> recentEvents = webhookEventRepository.findTop10ByOrderByCreatedAtDesc();
        return ResponseEntity.ok(recentEvents);
    }

    @GetMapping("/dlt")
    @Operation(summary = "List dead letter queue events")
    public ResponseEntity<List<WebhookEvent>> getDeadLetterQueueEvents() {
        List<WebhookEvent> dltEvents = deadLetterQueueService.getDeadLetterEvents();
        return ResponseEntity.ok(dltEvents);
    }

    @PostMapping("/dlt/{eventId}/retry")
    @Operation(summary = "Retry a dead letter event")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event queued for retry"),
            @ApiResponse(responseCode = "404", description = "Event not found"),
            @ApiResponse(responseCode = "400", description = "Event is not in a retriable state")
    })
    public ResponseEntity<Map<String, Object>> retryDeadLetterEvent(@PathVariable String eventId) {
        try {
            deadLetterQueueService.retryEvent(eventId);
            return ResponseEntity.ok(Map.of(
                    "status", "queued",
                    "event_id", eventId,
                    "message", "Event queued for retry"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/dlt/retry-all")
    @Operation(summary = "Retry all dead letter events")
    public ResponseEntity<Map<String, Object>> retryAllDeadLetterEvents() {
        int count = deadLetterQueueService.retryAllEvents();
        return ResponseEntity.ok(Map.of(
                "status", "queued",
                "events_queued", count,
                "message", count + " event(s) queued for retry"
        ));
    }

    @DeleteMapping("/dlt/{eventId}")
    @Operation(summary = "Discard a dead letter event")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event discarded"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<Map<String, Object>> discardDeadLetterEvent(@PathVariable String eventId) {
        try {
            deadLetterQueueService.discardEvent(eventId);
            return ResponseEntity.ok(Map.of(
                    "status", "discarded",
                    "event_id", eventId,
                    "message", "Event permanently discarded"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}

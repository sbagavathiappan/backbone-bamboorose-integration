package com.backbonebamboorose.controller;

import com.backbonebamboorose.model.WebhookEvent;
import com.backbonebamboorose.repository.WebhookEventRepository;
import com.backbonebamboorose.service.BambooRoseClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
public class InternalManagementController {

    private final WebhookEventRepository webhookEventRepository;
    private final BambooRoseClientService bambooRoseClientService;

    @GetMapping("/events")
    public ResponseEntity<Map<String, Object>> getEvents(
            @RequestParam(defaultValue = "0") int page,
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
    public ResponseEntity<List<WebhookEvent>> getEventsByStatus(@PathVariable String status) {
        WebhookEvent.EventStatus eventStatus = WebhookEvent.EventStatus.valueOf(status.toUpperCase());
        List<WebhookEvent> events = webhookEventRepository.findByStatus(eventStatus);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<WebhookEvent> getEventByEventId(@PathVariable String eventId) {
        return webhookEventRepository.findByEventId(eventId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/events/quote/{quoteId}")
    public ResponseEntity<List<WebhookEvent>> getEventsByQuoteId(@PathVariable String quoteId) {
        List<WebhookEvent> events = webhookEventRepository.findByQuoteId(quoteId);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/stats")
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
    public ResponseEntity<Map<String, Object>> checkExternalHealth() {
        Map<String, Object> health = new HashMap<>();
        boolean bambooRoseHealthy = bambooRoseClientService.healthCheck();
        health.put("bambooRose", bambooRoseHealthy ? "UP" : "DOWN");
        health.put("timestamp", java.time.OffsetDateTime.now().toString());
        return ResponseEntity.ok(health);
    }

    @GetMapping("/events/recent")
    public ResponseEntity<List<WebhookEvent>> getRecentEvents() {
        List<WebhookEvent> recentEvents = webhookEventRepository.findTop10ByOrderByCreatedAtDesc();
        return ResponseEntity.ok(recentEvents);
    }
}

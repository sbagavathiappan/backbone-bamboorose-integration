package com.backbonebamboorose.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("quote_id")
    private String quoteId;

    public static WebhookResponse success(String eventId, String quoteId, String message) {
        return WebhookResponse.builder()
                .success(true)
                .eventId(eventId)
                .quoteId(quoteId)
                .message(message)
                .build();
    }

    public static WebhookResponse failure(String eventId, String message) {
        return WebhookResponse.builder()
                .success(false)
                .eventId(eventId)
                .message(message)
                .build();
    }
}

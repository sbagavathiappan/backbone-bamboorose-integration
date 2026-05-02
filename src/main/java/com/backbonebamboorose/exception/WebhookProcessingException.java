package com.backbonebamboorose.exception;

public class WebhookProcessingException extends RuntimeException {

    private final String eventId;

    public WebhookProcessingException(String message, String eventId) {
        super(message);
        this.eventId = eventId;
    }

    public WebhookProcessingException(String message, String eventId, Throwable cause) {
        super(message, cause);
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}

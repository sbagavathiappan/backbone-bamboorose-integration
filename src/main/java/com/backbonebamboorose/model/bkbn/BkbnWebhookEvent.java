package com.backbonebamboorose.model.bkbn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BkbnWebhookEvent {

    @JsonProperty("event")
    private String event;

    @JsonProperty("timestamp")
    private OffsetDateTime timestamp;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("assignmentId")
    private String assignmentId;

    @JsonProperty("visualType")
    private String visualType;

    @JsonProperty("product")
    private String product;

    @JsonProperty("realEstatePropertyId")
    private String realEstatePropertyId;
}

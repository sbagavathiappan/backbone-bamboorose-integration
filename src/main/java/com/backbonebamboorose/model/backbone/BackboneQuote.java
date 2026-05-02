package com.backbonebamboorose.model.backbone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BackboneQuote {

    @JsonProperty("id")
    private String id;

    @JsonProperty("quote_number")
    private String quoteNumber;

    @JsonProperty("status")
    private String status;

    @JsonProperty("supplier_name")
    private String supplierName;

    @JsonProperty("supplier_id")
    private String supplierId;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    @JsonProperty("valid_until")
    private OffsetDateTime validUntil;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("line_items")
    private List<LineItem> lineItems;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("terms")
    private String terms;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LineItem {

        @JsonProperty("id")
        private String id;

        @JsonProperty("product_id")
        private String productId;

        @JsonProperty("product_name")
        private String productName;

        @JsonProperty("sku")
        private String sku;

        @JsonProperty("quantity")
        private Integer quantity;

        @JsonProperty("unit_price")
        private BigDecimal unitPrice;

        @JsonProperty("total_price")
        private BigDecimal totalPrice;

        @JsonProperty("moq")
        private Integer minimumOrderQuantity;

        @JsonProperty("lead_time_days")
        private Integer leadTimeDays;

        @JsonProperty("color")
        private String color;

        @JsonProperty("size")
        private String size;

        @JsonProperty("material")
        private String material;

        @JsonProperty("category")
        private String category;
    }
}

package com.backbonebamboorose.model.bamboorose;

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
public class BambooRoseQuote {

    @JsonProperty("quote_id")
    private String quoteId;

    @JsonProperty("external_quote_id")
    private String externalQuoteId;

    @JsonProperty("quote_number")
    private String quoteNumber;

    @JsonProperty("status")
    private String status;

    @JsonProperty("supplier_name")
    private String supplierName;

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

    @JsonProperty("terms_and_conditions")
    private String termsAndConditions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LineItem {

        @JsonProperty("line_item_id")
        private String lineItemId;

        @JsonProperty("external_line_item_id")
        private String externalLineItemId;

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

        @JsonProperty("minimum_order_quantity")
        private Integer minimumOrderQuantity;

        @JsonProperty("lead_time_days")
        private Integer leadTimeDays;

        @JsonProperty("specifications")
        private Specifications specifications;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Specifications {

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
}

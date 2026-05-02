package com.backbonebamboorose.transformer;

import com.backbonebamboorose.model.backbone.BackboneQuote;
import com.backbonebamboorose.model.bamboorose.BambooRoseQuote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuoteTransformerTest {

    private QuoteTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new QuoteTransformer();
    }

    @Test
    void transform_shouldMapBasicFields() {
        BackboneQuote backboneQuote = createTestBackboneQuote();

        BambooRoseQuote result = transformer.transform(backboneQuote);

        assertNotNull(result);
        assertEquals("Q-001", result.getExternalQuoteId());
        assertEquals("QT-2024-001", result.getQuoteNumber());
        assertEquals("DRAFT", result.getStatus());
        assertEquals("Test Supplier", result.getSupplierName());
        assertEquals("USD", result.getCurrency());
        assertEquals(new BigDecimal("1500.00"), result.getTotalAmount());
        assertEquals("Test notes", result.getNotes());
        assertEquals("Net 30", result.getTermsAndConditions());
    }

    @Test
    void transform_shouldMapStatusCorrectly() {
        assertEquals("DRAFT", transformer.transform(createQuoteWithStatus("CREATED")).getStatus());
        assertEquals("DRAFT", transformer.transform(createQuoteWithStatus("NEW")).getStatus());
        assertEquals("REVISION", transformer.transform(createQuoteWithStatus("UPDATED")).getStatus());
        assertEquals("APPROVED", transformer.transform(createQuoteWithStatus("APPROVED")).getStatus());
        assertEquals("APPROVED", transformer.transform(createQuoteWithStatus("ACCEPTED")).getStatus());
        assertEquals("REJECTED", transformer.transform(createQuoteWithStatus("REJECTED")).getStatus());
        assertEquals("EXPIRED", transformer.transform(createQuoteWithStatus("EXPIRED")).getStatus());
        assertEquals("PENDING_REVIEW", transformer.transform(createQuoteWithStatus("PENDING")).getStatus());
        assertEquals("DRAFT", transformer.transform(createQuoteWithStatus("UNKNOWN_STATUS")).getStatus());
        assertEquals("DRAFT", transformer.transform(createQuoteWithStatus(null)).getStatus());
    }

    @Test
    void transform_shouldMapLineItems() {
        BackboneQuote backboneQuote = createTestBackboneQuote();

        BambooRoseQuote result = transformer.transform(backboneQuote);

        assertNotNull(result.getLineItems());
        assertEquals(2, result.getLineItems().size());

        BambooRoseQuote.LineItem firstItem = result.getLineItems().get(0);
        assertEquals("LI-001", firstItem.getExternalLineItemId());
        assertEquals("Product A", firstItem.getProductName());
        assertEquals("SKU-001", firstItem.getSku());
        assertEquals(100, firstItem.getQuantity());
        assertEquals(new BigDecimal("10.00"), firstItem.getUnitPrice());
        assertEquals(new BigDecimal("1000.00"), firstItem.getTotalPrice());
        assertEquals(50, firstItem.getMinimumOrderQuantity());
        assertEquals(14, firstItem.getLeadTimeDays());

        assertNotNull(firstItem.getSpecifications());
        assertEquals("Red", firstItem.getSpecifications().getColor());
        assertEquals("M", firstItem.getSpecifications().getSize());
        assertEquals("Cotton", firstItem.getSpecifications().getMaterial());
        assertEquals("Apparel", firstItem.getSpecifications().getCategory());
    }

    @Test
    void transform_shouldHandleNullLineItems() {
        BackboneQuote backboneQuote = BackboneQuote.builder()
                .id("Q-001")
                .quoteNumber("QT-2024-001")
                .lineItems(null)
                .build();

        BambooRoseQuote result = transformer.transform(backboneQuote);

        assertNotNull(result);
        assertNotNull(result.getLineItems());
        assertTrue(result.getLineItems().isEmpty());
    }

    @Test
    void transform_shouldPreserveTimestamps() {
        OffsetDateTime now = OffsetDateTime.now();
        BackboneQuote backboneQuote = BackboneQuote.builder()
                .id("Q-001")
                .createdAt(now.minusDays(1))
                .updatedAt(now)
                .validUntil(now.plusDays(30))
                .build();

        BambooRoseQuote result = transformer.transform(backboneQuote);

        assertEquals(now.minusDays(1), result.getCreatedAt());
        assertEquals(now, result.getUpdatedAt());
        assertEquals(now.plusDays(30), result.getValidUntil());
    }

    private BackboneQuote createTestBackboneQuote() {
        return BackboneQuote.builder()
                .id("Q-001")
                .quoteNumber("QT-2024-001")
                .status("CREATED")
                .supplierName("Test Supplier")
                .currency("USD")
                .totalAmount(new BigDecimal("1500.00"))
                .notes("Test notes")
                .terms("Net 30")
                .lineItems(List.of(
                        createLineItem("LI-001", "Product A", "SKU-001", 100, new BigDecimal("10.00")),
                        createLineItem("LI-002", "Product B", "SKU-002", 50, new BigDecimal("10.00"))
                ))
                .build();
    }

    private BackboneQuote createQuoteWithStatus(String status) {
        return BackboneQuote.builder()
                .id("Q-001")
                .quoteNumber("QT-2024-001")
                .status(status)
                .build();
    }

    private BackboneQuote.LineItem createLineItem(String id, String name, String sku, int qty, BigDecimal price) {
        return BackboneQuote.LineItem.builder()
                .id(id)
                .productName(name)
                .sku(sku)
                .quantity(qty)
                .unitPrice(price)
                .totalPrice(price.multiply(BigDecimal.valueOf(qty)))
                .minimumOrderQuantity(50)
                .leadTimeDays(14)
                .color("Red")
                .size("M")
                .material("Cotton")
                .category("Apparel")
                .build();
    }
}

package com.backbonebamboorose.transformer;

import com.backbonebamboorose.model.bkbn.BkbnMaterialsResponse;
import com.backbonebamboorose.model.bkbn.BkbnWebhookEvent;
import com.backbonebamboorose.model.bamboorose.BambooRoseQuote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        BkbnWebhookEvent event = createTestEvent();
        BkbnMaterialsResponse materials = createTestMaterials();

        BambooRoseQuote result = transformer.transform(event, materials);

        assertNotNull(result);
        assertEquals("ORD-12345", result.getExternalQuoteId());
        assertEquals("BKBN-ORD-12345", result.getQuoteNumber());
        assertEquals("VISUALS_READY", result.getStatus());
        assertEquals("BKBN", result.getSupplierName());
        assertEquals("USD", result.getCurrency());
    }

    @Test
    void transform_shouldMapMaterialsToLineItems() {
        BkbnWebhookEvent event = createTestEvent();
        BkbnMaterialsResponse materials = createTestMaterials();

        BambooRoseQuote result = transformer.transform(event, materials);

        assertNotNull(result.getLineItems());
        assertEquals(2, result.getLineItems().size());

        BambooRoseQuote.LineItem firstItem = result.getLineItems().get(0);
        assertEquals("MAT-001", firstItem.getExternalLineItemId());
        assertEquals("Floor Plan", firstItem.getProductName());
        assertEquals("MAT-001", firstItem.getSku());
        assertNotNull(firstItem.getSpecifications());
        assertEquals("POST", firstItem.getSpecifications().getCategory());
    }

    @Test
    void transform_shouldHandleNullMaterials() {
        BkbnWebhookEvent event = createTestEvent();

        BambooRoseQuote result = transformer.transform(event, null);

        assertNotNull(result);
        assertNotNull(result.getLineItems());
        assertTrue(result.getLineItems().isEmpty());
    }

    @Test
    void transform_shouldPreserveTimestamp() {
        BkbnWebhookEvent event = createTestEvent();
        BkbnMaterialsResponse materials = createTestMaterials();

        BambooRoseQuote result = transformer.transform(event, materials);

        assertEquals(event.getTimestamp(), result.getCreatedAt());
        assertEquals(event.getTimestamp(), result.getUpdatedAt());
    }

    @Test
    void transform_shouldIncludeEventInfoInNotes() {
        BkbnWebhookEvent event = createTestEvent();
        BkbnMaterialsResponse materials = createTestMaterials();

        BambooRoseQuote result = transformer.transform(event, materials);

        assertNotNull(result.getNotes());
        assertTrue(result.getNotes().contains("VISUALS_READY"));
        assertTrue(result.getNotes().contains("ORD-12345"));
        assertTrue(result.getNotes().contains("ASM-001"));
    }

    private BkbnWebhookEvent createTestEvent() {
        return BkbnWebhookEvent.builder()
                .event("VISUALS_READY")
                .timestamp(OffsetDateTime.now())
                .orderId("ORD-12345")
                .assignmentId("ASM-001")
                .visualType("POST")
                .product("GROUND_PHOTO")
                .realEstatePropertyId("RE-789")
                .build();
    }

    private BkbnMaterialsResponse createTestMaterials() {
        return BkbnMaterialsResponse.builder()
                .orderId("ORD-12345")
                .assignmentId("ASM-001")
                .materials(List.of(
                        BkbnMaterialsResponse.Material.builder()
                                .id("MAT-001")
                                .name("Floor Plan")
                                .type("POST")
                                .url("https://example.com/floorplan.pdf")
                                .size(1024000L)
                                .mimeType("application/pdf")
                                .build(),
                        BkbnMaterialsResponse.Material.builder()
                                .id("MAT-002")
                                .name("Ground Photo")
                                .type("POST")
                                .url("https://example.com/photo.jpg")
                                .size(2048000L)
                                .mimeType("image/jpeg")
                                .build()
                ))
                .build();
    }
}

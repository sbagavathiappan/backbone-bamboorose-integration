package com.backbonebamboorose.transformer;

import com.backbonebamboorose.model.bkbn.BkbnMaterialsResponse;
import com.backbonebamboorose.model.bkbn.BkbnWebhookEvent;
import com.backbonebamboorose.model.bamboorose.BambooRoseQuote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class QuoteTransformer {

    public BambooRoseQuote transform(BkbnWebhookEvent bkbnEvent, BkbnMaterialsResponse materials) {
        log.debug("Transforming BKBN event to Bamboo Rose format: orderId={}", bkbnEvent.getOrderId());

        List<BambooRoseQuote.LineItem> lineItems = transformMaterialsToLineItems(materials);

        return BambooRoseQuote.builder()
                .externalQuoteId(bkbnEvent.getOrderId())
                .quoteNumber("BKBN-" + bkbnEvent.getOrderId())
                .status("VISUALS_READY")
                .supplierName("BKBN")
                .createdAt(bkbnEvent.getTimestamp())
                .updatedAt(bkbnEvent.getTimestamp())
                .currency("USD")
                .lineItems(lineItems)
                .notes(String.format("BKBN %s event for order %s, assignment %s",
                        bkbnEvent.getEvent(), bkbnEvent.getOrderId(), bkbnEvent.getAssignmentId()))
                .termsAndConditions(null)
                .build();
    }

    private List<BambooRoseQuote.LineItem> transformMaterialsToLineItems(BkbnMaterialsResponse materials) {
        if (materials == null || materials.getMaterials() == null) {
            return Collections.emptyList();
        }

        return materials.getMaterials().stream()
                .map(this::transformMaterial)
                .collect(Collectors.toList());
    }

    private BambooRoseQuote.LineItem transformMaterial(BkbnMaterialsResponse.Material material) {
        BambooRoseQuote.LineItem.Specifications specs = BambooRoseQuote.LineItem.Specifications.builder()
                .category(material.getType())
                .build();

        return BambooRoseQuote.LineItem.builder()
                .externalLineItemId(material.getId())
                .productName(material.getName())
                .sku(material.getId())
                .quantity(1)
                .unitPrice(BigDecimal.ZERO)
                .totalPrice(BigDecimal.ZERO)
                .specifications(specs)
                .build();
    }
}

package com.backbonebamboorose.transformer;

import com.backbonebamboorose.model.backbone.BackboneQuote;
import com.backbonebamboorose.model.bamboorose.BambooRoseQuote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
public class QuoteTransformer {

    public BambooRoseQuote transform(BackboneQuote backboneQuote) {
        log.debug("Transforming Backbone quote {} to Bamboo Rose format", backboneQuote.getQuoteNumber());

        return BambooRoseQuote.builder()
                .externalQuoteId(backboneQuote.getId())
                .quoteNumber(backboneQuote.getQuoteNumber())
                .status(mapStatus(backboneQuote.getStatus()))
                .supplierName(backboneQuote.getSupplierName())
                .createdAt(backboneQuote.getCreatedAt())
                .updatedAt(backboneQuote.getUpdatedAt())
                .validUntil(backboneQuote.getValidUntil())
                .currency(backboneQuote.getCurrency())
                .totalAmount(backboneQuote.getTotalAmount())
                .lineItems(transformLineItems(backboneQuote.getLineItems()))
                .notes(backboneQuote.getNotes())
                .termsAndConditions(backboneQuote.getTerms())
                .build();
    }

    private String mapStatus(String backboneStatus) {
        if (backboneStatus == null) {
            return "DRAFT";
        }
        return switch (backboneStatus.toUpperCase()) {
            case "CREATED", "NEW" -> "DRAFT";
            case "UPDATED", "REVISED" -> "REVISION";
            case "APPROVED", "ACCEPTED" -> "APPROVED";
            case "REJECTED", "DECLINED" -> "REJECTED";
            case "EXPIRED" -> "EXPIRED";
            case "PENDING" -> "PENDING_REVIEW";
            default -> "DRAFT";
        };
    }

    private java.util.List<BambooRoseQuote.LineItem> transformLineItems(
            java.util.List<BackboneQuote.LineItem> lineItems) {
        if (lineItems == null) {
            return java.util.Collections.emptyList();
        }

        return lineItems.stream()
                .map(this::transformLineItem)
                .collect(Collectors.toList());
    }

    private BambooRoseQuote.LineItem transformLineItem(BackboneQuote.LineItem backboneItem) {
        BambooRoseQuote.LineItem.Specifications specs = BambooRoseQuote.LineItem.Specifications.builder()
                .color(backboneItem.getColor())
                .size(backboneItem.getSize())
                .material(backboneItem.getMaterial())
                .category(backboneItem.getCategory())
                .build();

        return BambooRoseQuote.LineItem.builder()
                .externalLineItemId(backboneItem.getId())
                .productName(backboneItem.getProductName())
                .sku(backboneItem.getSku())
                .quantity(backboneItem.getQuantity())
                .unitPrice(backboneItem.getUnitPrice())
                .totalPrice(backboneItem.getTotalPrice())
                .minimumOrderQuantity(backboneItem.getMinimumOrderQuantity())
                .leadTimeDays(backboneItem.getLeadTimeDays())
                .specifications(specs)
                .build();
    }
}

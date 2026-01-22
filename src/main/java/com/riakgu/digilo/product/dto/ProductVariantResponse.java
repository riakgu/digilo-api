package com.riakgu.digilo.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.riakgu.digilo.product.DeliveryType;
import com.riakgu.digilo.product.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductVariantResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String sku;
    private String name;
    private BigDecimal price;
    private DeliveryType deliveryType;
    private Integer durationDays;
    private Integer warrantyDays;
    private Boolean isActive;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProductVariantResponse fromEntity(ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .productId(variant.getProduct().getId())
                .productName(variant.getProduct().getName())
                .sku(variant.getSku())
                .name(variant.getName())
                .price(variant.getPrice())
                .deliveryType(variant.getDeliveryType())
                .durationDays(variant.getDurationDays())
                .warrantyDays(variant.getWarrantyDays())
                .isActive(variant.getIsActive())
                .metadata(variant.getMetadata())
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt())
                .build();
    }

    public static ProductVariantResponse fromEntitySimple(ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .name(variant.getName())
                .price(variant.getPrice())
                .deliveryType(variant.getDeliveryType())
                .durationDays(variant.getDurationDays())
                .warrantyDays(variant.getWarrantyDays())
                .isActive(variant.getIsActive())
                .build();
    }
}

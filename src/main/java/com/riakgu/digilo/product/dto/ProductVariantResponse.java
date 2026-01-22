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
    private Integer availableStock;
    private Boolean isAvailable;
    private Boolean isActive;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProductVariantResponse fromEntity(ProductVariant variant, long availableStock) {
        boolean isAvailable = switch (variant.getDeliveryType()) {
            case AUTO -> availableStock > 0;
            case MANUAL, HYBRID -> true;
        };
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
                .availableStock((int) availableStock)
                .isAvailable(isAvailable)
                .isActive(variant.getIsActive())
                .metadata(variant.getMetadata())
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt())
                .build();
    }
}

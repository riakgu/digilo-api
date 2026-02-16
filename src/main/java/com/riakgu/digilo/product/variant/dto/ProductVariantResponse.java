package com.riakgu.digilo.product.variant.dto;

import com.riakgu.digilo.product.variant.DeliveryType;
import com.riakgu.digilo.product.variant.ProductVariant;
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
public class ProductVariantResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private String productImageUrl;
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
        return fromEntity(variant, availableStock, null);
    }

    public static ProductVariantResponse fromEntity(ProductVariant variant, long availableStock, String imageUrl) {
        boolean isAvailable = switch (variant.getDeliveryType()) {
            case AUTO -> availableStock > 0;
            case MANUAL, HYBRID -> true;
        };
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .productId(variant.getProduct().getId())
                .productName(variant.getProduct().getName())
                .productSlug(variant.getProduct().getSlug())
                .productImageUrl(imageUrl)
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

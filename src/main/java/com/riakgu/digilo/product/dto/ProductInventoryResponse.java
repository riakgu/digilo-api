package com.riakgu.digilo.product.dto;

import com.riakgu.digilo.product.InventoryStatus;
import com.riakgu.digilo.product.ProductInventory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductInventoryResponse {

    private Long id;
    private Long variantId;
    private String variantSku;
    private String variantName;
    private InventoryStatus status;
    private Map<String, Object> credential;
    private Long orderItemId;
    private Instant reservedAt;
    private Instant soldAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProductInventoryResponse fromEntity(ProductInventory inventory) {
        return ProductInventoryResponse.builder()
                .id(inventory.getId())
                .variantId(inventory.getVariant().getId())
                .variantSku(inventory.getVariant().getSku())
                .variantName(inventory.getVariant().getName())
                .status(inventory.getStatus())
                .orderItemId(inventory.getOrderItemId())
                .reservedAt(inventory.getReservedAt())
                .soldAt(inventory.getSoldAt())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    public static ProductInventoryResponse fromEntityWithCredential(
            ProductInventory inventory,
            Map<String, Object> decryptedCredential
    ) {
        return ProductInventoryResponse.builder()
                .id(inventory.getId())
                .variantId(inventory.getVariant().getId())
                .variantSku(inventory.getVariant().getSku())
                .variantName(inventory.getVariant().getName())
                .status(inventory.getStatus())
                .credential(decryptedCredential)
                .orderItemId(inventory.getOrderItemId())
                .reservedAt(inventory.getReservedAt())
                .soldAt(inventory.getSoldAt())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}
package com.riakgu.digilo.product.inventory;

import com.riakgu.digilo.common.base.BaseEntity;
import com.riakgu.digilo.product.variant.ProductVariant;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_inventories", indexes = {
        @Index(name = "idx_inventory_variant_status", columnList = "variant_id, status"),
        @Index(name = "idx_inventory_order_item", columnList = "order_item_id")
})
public class ProductInventory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "credential", columnDefinition = "TEXT")
    private String credential;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InventoryStatus status = InventoryStatus.AVAILABLE;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "reserved_at")
    private Instant reservedAt;

    @Column(name = "sold_at")
    private Instant soldAt;
}
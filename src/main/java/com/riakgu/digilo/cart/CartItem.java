package com.riakgu.digilo.cart;

import com.riakgu.digilo.common.base.BaseEntity;
import com.riakgu.digilo.product.ProductVariant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cart_items", 
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"cart_id", "variant_id"})
    },
    indexes = {
        @Index(name = "idx_cart_item_cart_id", columnList = "cart_id")
    }
)
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;
}

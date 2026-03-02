package com.riakgu.digilo.cart.dto;

import com.riakgu.digilo.cart.CartItem;
import com.riakgu.digilo.product.variant.DeliveryType;
import com.riakgu.digilo.product.variant.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartItemResponse {

    private Long id;
    private Long variantId;
    private String variantName;
    private BigDecimal price;
    private Long productId;
    private String productName;
    private String productSlug;
    private String productImageUrl;
    private Integer quantity;
    private BigDecimal subtotal;
    private Boolean isAvailable;
    private Integer availableStock;

    public static CartItemResponse fromEntity(CartItem item, long availableStock, String imageUrl) {
        ProductVariant variant = item.getVariant();
        boolean isAvailable = switch (variant.getDeliveryType()) {
            case AUTO -> availableStock > 0;
            case MANUAL, HYBRID -> true;
        };
        BigDecimal subtotal = variant.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponse.builder()
                .id(item.getId())
                .variantId(variant.getId())
                .variantName(variant.getName())
                .price(variant.getPrice())
                .productId(variant.getProduct().getId())
                .productName(variant.getProduct().getName())
                .productSlug(variant.getProduct().getSlug())
                .productImageUrl(imageUrl)
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .isAvailable(isAvailable)
                .availableStock((int) availableStock)
                .build();
    }
}

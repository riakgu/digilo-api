package com.riakgu.digilo.cart.dto;


import com.riakgu.digilo.cart.CartItem;
import com.riakgu.digilo.product.dto.ProductVariantResponse;
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
    private ProductVariantResponse variant;
    private Integer quantity;
    private BigDecimal subtotal;

    public static CartItemResponse fromEntity(CartItem item, ProductVariantResponse variantResponse) {
        BigDecimal subtotal = variantResponse.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        return CartItemResponse.builder()
                .id(item.getId())
                .variant(variantResponse)
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}

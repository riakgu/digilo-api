package com.riakgu.digilo.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartResponse {

    private Long id;
    private List<CartItemResponse> items;
    private Integer totalItems;
    private BigDecimal totalPrice;

    public static CartResponse fromItems(Long cartId, List<CartItemResponse> items) {
        int totalItems = items.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();
        BigDecimal totalPrice = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return CartResponse.builder()
                .id(cartId)
                .items(items)
                .totalItems(totalItems)
                .totalPrice(totalPrice)
                .build();
    }
}
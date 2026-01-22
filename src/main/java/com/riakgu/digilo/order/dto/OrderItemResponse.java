package com.riakgu.digilo.order.dto;

import com.riakgu.digilo.order.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItemResponse {

    private Long id;
    private Long variantId;
    private String variantName;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;

    public static OrderItemResponse fromEntity(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .variantId(item.getVariant().getId())
                .variantName(item.getVariantName())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }
}
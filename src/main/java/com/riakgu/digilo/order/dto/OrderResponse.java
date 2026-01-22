package com.riakgu.digilo.order.dto;

import com.riakgu.digilo.order.Order;
import com.riakgu.digilo.order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String notes;
    private List<OrderItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;

    public static OrderResponse fromEntity(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::fromEntity)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .notes(order.getNotes())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
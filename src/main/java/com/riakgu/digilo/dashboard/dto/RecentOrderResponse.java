package com.riakgu.digilo.dashboard.dto;

import com.riakgu.digilo.order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentOrderResponse {

    private Long id;
    private String orderNumber;
    private String userName;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private Instant createdAt;
}

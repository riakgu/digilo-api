package com.riakgu.digilo.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private Long totalUsers;
    private Long activeUsers;
    private Long totalProducts;
    private Long activeProducts;
    private Long totalOrders;
    private Long pendingOrders;
    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
}

package com.riakgu.digilo.dashboard;

import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.dashboard.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats() {
        DashboardStatsResponse stats = dashboardService.getStats();
        return ResponseEntity.ok(ApiResponse.success("OK", "Dashboard stats retrieved", stats));
    }

    @GetMapping("/top-users")
    public ResponseEntity<ApiResponse<List<TopUserResponse>>> getTopUsers(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<TopUserResponse> users = dashboardService.getTopUsers(limit);
        return ResponseEntity.ok(ApiResponse.success("OK", "Top users retrieved", users));
    }

    @GetMapping("/top-products")
    public ResponseEntity<ApiResponse<List<TopProductResponse>>> getTopProducts(
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<TopProductResponse> products = dashboardService.getTopProducts(limit);
        return ResponseEntity.ok(ApiResponse.success("OK", "Top products retrieved", products));
    }

    @GetMapping("/recent-orders")
    public ResponseEntity<ApiResponse<List<RecentOrderResponse>>> getRecentOrders(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<RecentOrderResponse> orders = dashboardService.getRecentOrders(limit);
        return ResponseEntity.ok(ApiResponse.success("OK", "Recent orders retrieved", orders));
    }

    @GetMapping("/sales-chart")
    public ResponseEntity<ApiResponse<List<SalesChartResponse>>> getSalesChart(
            @RequestParam(defaultValue = "7d") String period
    ) {
        List<SalesChartResponse> chart = dashboardService.getSalesChart(period);
        return ResponseEntity.ok(ApiResponse.success("OK", "Sales chart data retrieved", chart));
    }
}

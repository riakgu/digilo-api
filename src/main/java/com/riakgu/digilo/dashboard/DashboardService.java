package com.riakgu.digilo.dashboard;

import com.riakgu.digilo.config.CacheConfig;
import com.riakgu.digilo.dashboard.dto.*;
import com.riakgu.digilo.order.Order;
import com.riakgu.digilo.order.OrderRepository;
import com.riakgu.digilo.product.ProductImageHelper;
import com.riakgu.digilo.product.ProductRepository;
import com.riakgu.digilo.user.UserRepository;
import com.riakgu.digilo.user.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ProductImageHelper productImageHelper;
    private final EntityManager entityManager;

    private static final int MAX_LIMIT = 50;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.DASHBOARD_STATS_CACHE)
    public DashboardStatsResponse getStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long totalProducts = productRepository.count();
        long activeProducts = productRepository.countByIsActive(true);

        // Order stats
        Query orderStatsQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*), " +
                "SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END), " +
                "COALESCE(SUM(CASE WHEN status IN ('PAID', 'COMPLETED') THEN total_amount ELSE 0 END), 0), " +
                "COALESCE(SUM(CASE WHEN status IN ('PAID', 'COMPLETED') AND created_at >= :today THEN total_amount ELSE 0 END), 0) " +
                "FROM orders"
        );
        orderStatsQuery.setParameter("today", Instant.now().truncatedTo(ChronoUnit.DAYS));

        Object[] result = (Object[]) orderStatsQuery.getSingleResult();
        long totalOrders = ((Number) result[0]).longValue();
        long pendingOrders = ((Number) result[1]).longValue();
        BigDecimal totalRevenue = result[2] != null ? new BigDecimal(result[2].toString()) : BigDecimal.ZERO;
        BigDecimal todayRevenue = result[3] != null ? new BigDecimal(result[3].toString()) : BigDecimal.ZERO;

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .totalRevenue(totalRevenue)
                .todayRevenue(todayRevenue)
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.DASHBOARD_TOP_USERS_CACHE, key = "#limit")
    public List<TopUserResponse> getTopUsers(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        Query query = entityManager.createNativeQuery(
                "SELECT u.id, u.name, u.email, COUNT(o.id) as order_count, COALESCE(SUM(o.total_amount), 0) as total_spending " +
                "FROM users u " +
                "JOIN orders o ON o.user_id = u.id " +
                "WHERE o.status IN ('PAID', 'COMPLETED') " +
                "GROUP BY u.id " +
                "ORDER BY total_spending DESC " +
                "LIMIT :limit"
        );
        query.setParameter("limit", safeLimit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream().map(row -> TopUserResponse.builder()
                .id(((Number) row[0]).longValue())
                .name((String) row[1])
                .email((String) row[2])
                .totalOrders(((Number) row[3]).longValue())
                .totalSpending(new BigDecimal(row[4].toString()))
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.DASHBOARD_TOP_PRODUCTS_CACHE, key = "#limit")
    public List<TopProductResponse> getTopProducts(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        Query query = entityManager.createNativeQuery(
                "SELECT p.id, p.name, p.slug, COALESCE(SUM(oi.quantity), 0) as total_sold, " +
                "COALESCE(SUM(oi.quantity * oi.price), 0) as total_revenue " +
                "FROM products p " +
                "JOIN product_variants v ON v.product_id = p.id " +
                "JOIN order_items oi ON oi.variant_id = v.id " +
                "JOIN orders o ON o.id = oi.order_id AND o.status IN ('PAID', 'COMPLETED') " +
                "WHERE p.is_active = true " +
                "GROUP BY p.id " +
                "ORDER BY total_sold DESC " +
                "LIMIT :limit"
        );
        query.setParameter("limit", safeLimit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream().map(row -> {
            Long productId = ((Number) row[0]).longValue();
            return TopProductResponse.builder()
                    .id(productId)
                    .name((String) row[1])
                    .slug((String) row[2])
                    .imageUrl(getProductImageUrl(productId))
                    .totalSold(((Number) row[3]).longValue())
                    .totalRevenue(new BigDecimal(row[4].toString()))
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.DASHBOARD_RECENT_ORDERS_CACHE, key = "#limit")
    public List<RecentOrderResponse> getRecentOrders(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<Order> orders = orderRepository.findAll(PageRequest.of(0, safeLimit, 
                Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();

        return orders.stream().map(order -> RecentOrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userName(order.getUser().getName())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SalesChartResponse> getSalesChart(String period) {
        int days = switch (period) {
            case "30d" -> 30;
            case "14d" -> 14;
            default -> 7;
        };

        LocalDate startDate = LocalDate.now().minusDays(days - 1);
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        Query query = entityManager.createNativeQuery(
                "SELECT DATE(created_at) as order_date, COUNT(*) as order_count, COALESCE(SUM(total_amount), 0) as revenue " +
                "FROM orders " +
                "WHERE status IN ('PAID', 'COMPLETED') AND created_at >= :startDate " +
                "GROUP BY DATE(created_at) " +
                "ORDER BY order_date ASC"
        );
        query.setParameter("startDate", startInstant);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream().map(row -> SalesChartResponse.builder()
                .date(((Date) row[0]).toLocalDate())
                .orderCount(((Number) row[1]).longValue())
                .revenue(new BigDecimal(row[2].toString()))
                .build()
        ).collect(Collectors.toList());
    }

    private String getProductImageUrl(Long productId) {
        return productRepository.findById(productId)
                .map(productImageHelper::getDisplayImageUrl)
                .orElse(null);
    }
}

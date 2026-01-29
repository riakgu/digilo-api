package com.riakgu.digilo.promo;

import com.riakgu.digilo.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "promos", indexes = {
        @Index(name = "idx_promo_code", columnList = "code"),
        @Index(name = "idx_promo_is_active", columnList = "is_active")
})
public class Promo extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "min_order_amount", precision = 12, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "max_total_usage")
    private Integer maxTotalUsage;

    @Column(name = "max_usage_per_user")
    private Integer maxUsagePerUser = 1;

    @Column(name = "used_count")
    private Integer usedCount = 0;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
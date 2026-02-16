package com.riakgu.digilo.promo.dto;

import com.riakgu.digilo.promo.DiscountType;
import com.riakgu.digilo.promo.Promo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PromoResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscount;
    private BigDecimal minOrderAmount;
    private Integer maxTotalUsage;
    private Integer maxUsagePerUser;
    private Long usedCount;
    private Instant startsAt;
    private Instant expiresAt;
    private Boolean isActive;
    private Instant createdAt;

    public static PromoResponse fromEntity(Promo promo, long usedCount) {
        return PromoResponse.builder()
                .id(promo.getId())
                .code(promo.getCode())
                .name(promo.getName())
                .description(promo.getDescription())
                .discountType(promo.getDiscountType())
                .discountValue(promo.getDiscountValue())
                .maxDiscount(promo.getMaxDiscount())
                .minOrderAmount(promo.getMinOrderAmount())
                .maxTotalUsage(promo.getMaxTotalUsage())
                .maxUsagePerUser(promo.getMaxUsagePerUser())
                .usedCount(usedCount)
                .startsAt(promo.getStartsAt())
                .expiresAt(promo.getExpiresAt())
                .isActive(promo.getIsActive())
                .createdAt(promo.getCreatedAt())
                .build();
    }
}
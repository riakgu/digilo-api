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
public class PublicPromoResponse {

    private String code;
    private String name;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscount;
    private BigDecimal minOrderAmount;
    private Instant expiresAt;

    public static PublicPromoResponse fromEntity(Promo promo) {
        return PublicPromoResponse.builder()
                .code(promo.getCode())
                .name(promo.getName())
                .description(promo.getDescription())
                .discountType(promo.getDiscountType())
                .discountValue(promo.getDiscountValue())
                .maxDiscount(promo.getMaxDiscount())
                .minOrderAmount(promo.getMinOrderAmount())
                .expiresAt(promo.getExpiresAt())
                .build();
    }
}

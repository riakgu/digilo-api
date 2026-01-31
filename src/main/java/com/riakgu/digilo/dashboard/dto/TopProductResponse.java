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
public class TopProductResponse {

    private Long id;
    private String name;
    private String slug;
    private String imageUrl;
    private Long totalSold;
    private BigDecimal totalRevenue;
}

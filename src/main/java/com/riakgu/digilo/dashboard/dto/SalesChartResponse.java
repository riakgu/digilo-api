package com.riakgu.digilo.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesChartResponse {

    private LocalDate date;
    private Long orderCount;
    private BigDecimal revenue;
}

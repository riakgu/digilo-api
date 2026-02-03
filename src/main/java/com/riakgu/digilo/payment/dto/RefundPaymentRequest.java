package com.riakgu.digilo.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefundPaymentRequest {

    @NotBlank(message = "Refund notes is required")
    private String notes;
}

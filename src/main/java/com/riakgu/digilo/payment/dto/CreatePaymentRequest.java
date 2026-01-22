package com.riakgu.digilo.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    private String paymentType = "qris";
}
package com.riakgu.digilo.payment.dto;

import com.riakgu.digilo.payment.Payment;
import com.riakgu.digilo.payment.PaymentStatus;
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
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private String orderNumber;
    private String provider;
    private String paymentType;
    private String providerOrderId;
    private String providerTransactionId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String qrCodeUrl;
    private Instant paidAt;
    private Instant expiredAt;
    private Instant createdAt;

    public static PaymentResponse fromEntity(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .orderNumber(payment.getOrder().getOrderNumber())
                .provider(payment.getProvider())
                .paymentType(payment.getPaymentType())
                .providerOrderId(payment.getProviderOrderId())
                .providerTransactionId(payment.getProviderTransactionId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .qrCodeUrl(payment.getQrCodeUrl())
                .paidAt(payment.getPaidAt())
                .expiredAt(payment.getExpiredAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
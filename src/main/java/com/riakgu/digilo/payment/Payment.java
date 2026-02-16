package com.riakgu.digilo.payment;

import com.riakgu.digilo.common.base.BaseEntity;
import com.riakgu.digilo.order.Order;
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
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order_id", columnList = "order_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_provider_order_id", columnList = "provider_order_id")
})
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String provider = "MIDTRANS";

    @Column(name = "payment_type", nullable = false, length = 30)
    private String paymentType;

    @Column(name = "provider_order_id", nullable = false, unique = true, length = 100)
    private String providerOrderId;

    @Column(name = "provider_transaction_id", length = 100)
    private String providerTransactionId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "IDR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "qr_code_url", columnDefinition = "TEXT")
    private String qrCodeUrl;
}

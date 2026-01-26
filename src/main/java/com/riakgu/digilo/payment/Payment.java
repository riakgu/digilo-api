package com.riakgu.digilo.payment;

import com.riakgu.digilo.common.base.BaseEntity;
import com.riakgu.digilo.order.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payments")
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 30)
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
    private String currency = "IDR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "qr_code_url", columnDefinition = "TEXT")
    private String qrCodeUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_charge_response", columnDefinition = "jsonb")
    private Map<String, Object> rawChargeResponse;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_notification_payload", columnDefinition = "jsonb")
    private Map<String, Object> rawNotificationPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_status_response", columnDefinition = "jsonb")
    private Map<String, Object> rawStatusResponse;
}
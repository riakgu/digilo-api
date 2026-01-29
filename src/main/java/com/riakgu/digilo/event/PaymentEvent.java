package com.riakgu.digilo.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published when payment state changes.
 * Topics: digilo.payments
 */
public record PaymentEvent(
        String eventType,
        String orderNumber,
        Long paymentId,
        String paymentStatus,
        BigDecimal amount,
        Instant timestamp
) {
    public static final String TOPIC = "digilo.payments";
    
    public static final String PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String PAYMENT_EXPIRED = "PAYMENT_EXPIRED";
    public static final String PAYMENT_CREATED = "PAYMENT_CREATED";
    
    public static PaymentEvent paymentCreated(String orderNumber, Long paymentId, BigDecimal amount) {
        return new PaymentEvent(
                PAYMENT_CREATED,
                orderNumber,
                paymentId,
                "PENDING",
                amount,
                Instant.now()
        );
    }
    
    public static PaymentEvent paymentSuccess(String orderNumber, Long paymentId, BigDecimal amount) {
        return new PaymentEvent(
                PAYMENT_SUCCESS,
                orderNumber,
                paymentId,
                "SUCCESS",
                amount,
                Instant.now()
        );
    }
    
    public static PaymentEvent paymentFailed(String orderNumber, Long paymentId, BigDecimal amount) {
        return new PaymentEvent(
                PAYMENT_FAILED,
                orderNumber,
                paymentId,
                "FAILED",
                amount,
                Instant.now()
        );
    }
    
    public static PaymentEvent paymentExpired(String orderNumber, Long paymentId, BigDecimal amount) {
        return new PaymentEvent(
                PAYMENT_EXPIRED,
                orderNumber,
                paymentId,
                "EXPIRED",
                amount,
                Instant.now()
        );
    }
}

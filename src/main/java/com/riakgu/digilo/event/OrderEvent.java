package com.riakgu.digilo.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published when order state changes.
 * Topics: digilo.orders
 */
public record OrderEvent(
        String eventType,
        String orderNumber,
        Long userId,
        BigDecimal totalAmount,
        String status,
        Instant timestamp
) {
    public static final String TOPIC = "digilo.orders";
    
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_PAID = "ORDER_PAID";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String ORDER_FAILED = "ORDER_FAILED";
    
    public static OrderEvent orderCreated(String orderNumber, Long userId, BigDecimal totalAmount) {
        return new OrderEvent(
                ORDER_CREATED,
                orderNumber,
                userId,
                totalAmount,
                "PENDING",
                Instant.now()
        );
    }
    
    public static OrderEvent orderPaid(String orderNumber, Long userId, BigDecimal totalAmount) {
        return new OrderEvent(
                ORDER_PAID,
                orderNumber,
                userId,
                totalAmount,
                "PAID",
                Instant.now()
        );
    }
    
    public static OrderEvent orderCancelled(String orderNumber, Long userId, BigDecimal totalAmount) {
        return new OrderEvent(
                ORDER_CANCELLED,
                orderNumber,
                userId,
                totalAmount,
                "CANCELLED",
                Instant.now()
        );
    }
    
    public static OrderEvent orderFailed(String orderNumber, Long userId, BigDecimal totalAmount) {
        return new OrderEvent(
                ORDER_FAILED,
                orderNumber,
                userId,
                totalAmount,
                "FAILED",
                Instant.now()
        );
    }

    public static final String ORDER_COMPLETED = "ORDER_COMPLETED";

    public static OrderEvent orderCompleted(String orderNumber, Long userId, BigDecimal totalAmount) {
        return new OrderEvent(
                ORDER_COMPLETED,
                orderNumber,
                userId,
                totalAmount,
                "COMPLETED",
                Instant.now()
        );
    }
}

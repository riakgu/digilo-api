package com.riakgu.digilo.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Sample consumer that logs all events.
 * This is a template for future consumers like email notifications, analytics, etc.
 */
@Slf4j
@Component
public class EventLoggingConsumer {

    @KafkaListener(
            topics = "digilo.orders",
            groupId = "digilo-logging-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvent(OrderEvent event) {
        log.info("[KAFKA] Order Event: type={}, orderNumber={}, userId={}, amount={}, status={}",
                event.eventType(),
                event.orderNumber(),
                event.userId(),
                event.totalAmount(),
                event.status());
    }

    @KafkaListener(
            topics = "digilo.payments",
            groupId = "digilo-logging-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("[KAFKA] Payment Event: type={}, orderNumber={}, paymentId={}, status={}, amount={}",
                event.eventType(),
                event.orderNumber(),
                event.paymentId(),
                event.paymentStatus(),
                event.amount());
    }
}

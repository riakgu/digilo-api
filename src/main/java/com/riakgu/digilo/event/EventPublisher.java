package com.riakgu.digilo.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing events to Kafka topics.
 * All event publishing is asynchronous and non-blocking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish an order event to the orders topic.
     */
    public void publishOrderEvent(OrderEvent event) {
        publish(OrderEvent.TOPIC, event.orderNumber(), event);
    }

    /**
     * Publish a payment event to the payments topic.
     */
    public void publishPaymentEvent(PaymentEvent event) {
        publish(PaymentEvent.TOPIC, event.orderNumber(), event);
    }

    /**
     * Generic publish method with logging and error handling.
     */
    private void publish(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);
        
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event to topic {}: {}", topic, ex.getMessage(), ex);
            } else {
                log.debug("Event published to topic {}: partition={}, offset={}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}

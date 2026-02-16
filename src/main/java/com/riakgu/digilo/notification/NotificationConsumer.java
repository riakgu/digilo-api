package com.riakgu.digilo.notification;

import com.riakgu.digilo.event.OrderEvent;
import com.riakgu.digilo.event.PaymentEvent;
import com.riakgu.digilo.order.Order;
import com.riakgu.digilo.order.OrderRepository;
import com.riakgu.digilo.order.OrderService;
import com.riakgu.digilo.payment.Payment;
import com.riakgu.digilo.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final NotificationSenderService notificationSenderService;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PaymentRepository paymentRepository;

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("id", "ID"));

    @KafkaListener(topics = "digilo.orders", groupId = "digilo-notification-group")
    public void handleOrderEvent(OrderEvent event) {
        log.info("[KAFKA] Order Event: type={}, orderNumber={}, userId={}",
                event.eventType(), event.orderNumber(), event.userId());

        Optional<Order> orderOpt = orderRepository.findByOrderNumberWithDetails(event.orderNumber());
        if (orderOpt.isEmpty()) {
            log.warn("Order not found for notification: {}", event.orderNumber());
            return;
        }

        Order order = orderOpt.get();
        Long orderId = order.getId();
        String amount = CURRENCY_FORMAT.format(event.totalAmount());

        switch (event.eventType()) {
            case OrderEvent.ORDER_CREATED -> notificationService.createNotification(
                    event.userId(),
                    NotificationType.ORDER_CREATED,
                    "Order Placed",
                    "Your order #" + event.orderNumber() + " (" + amount + ") has been created. Please complete the payment.",
                    ReferenceType.ORDER,
                    orderId
            );
            case OrderEvent.ORDER_PAID -> notificationService.createNotification(
                    event.userId(),
                    NotificationType.ORDER_PAID,
                    "Payment Received",
                    "Your order #" + event.orderNumber() + " is ready. View your credentials in order details.",
                    ReferenceType.ORDER,
                    orderId
            );
            case OrderEvent.ORDER_CANCELLED -> notificationService.createNotification(
                    event.userId(),
                    NotificationType.ORDER_CANCELLED,
                    "Order Cancelled",
                    "Your order #" + event.orderNumber() + " has been cancelled.",
                    ReferenceType.ORDER,
                    orderId
            );
            case OrderEvent.ORDER_FAILED -> notificationService.createNotification(
                    event.userId(),
                    NotificationType.ORDER_FAILED,
                    "Order Failed",
                    "Your order #" + event.orderNumber() + " could not be processed.",
                    ReferenceType.ORDER,
                    orderId
            );
            case OrderEvent.ORDER_COMPLETED -> {
                notificationService.createNotification(
                        event.userId(),
                        NotificationType.ORDER_COMPLETED,
                        "Order Completed",
                        "Your order #" + event.orderNumber() + " is complete. Your product credentials are ready.",
                        ReferenceType.ORDER,
                        orderId
                );

                try {
                    var credentials = orderService.getOrderCredentialsAdmin(orderId);
                    notificationSenderService.sendCredentialsDeliveryEmail(order, credentials);
                    notificationSenderService.sendCredentialsDeliveryWhatsApp(order, credentials);
                } catch (Exception e) {
                    log.error("Failed to send credentials delivery notification: {}", e.getMessage());
                }
            }
        }
    }

    @KafkaListener(topics = "digilo.payments", groupId = "digilo-notification-group")
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("[KAFKA] Payment Event: type={}, orderNumber={}, paymentId={}",
                event.eventType(), event.orderNumber(), event.paymentId());

        Optional<Order> orderOpt = orderRepository.findByOrderNumberWithDetails(event.orderNumber());
        if (orderOpt.isEmpty()) {
            log.warn("Order not found for payment notification: {}", event.orderNumber());
            return;
        }

        Order order = orderOpt.get();
        Long userId = order.getUser().getId();
        String amount = CURRENCY_FORMAT.format(event.amount());

        switch (event.eventType()) {
            case PaymentEvent.PAYMENT_CREATED -> notificationService.createNotification(
                    userId,
                    NotificationType.PAYMENT_CREATED,
                    "Payment Initiated",
                    "Payment of " + amount + " initiated for order #" + event.orderNumber() + ". Please complete within the time limit.",
                    ReferenceType.ORDER,
                    order.getId()
            );
            case PaymentEvent.PAYMENT_SUCCESS -> {
                notificationService.createNotification(
                        userId,
                        NotificationType.PAYMENT_SUCCESS,
                        "Payment Successful",
                        "Payment of " + amount + " for order #" + event.orderNumber() + " received.",
                        ReferenceType.ORDER,
                        order.getId()
                );

                Optional<Payment> paymentOpt = paymentRepository.findById(event.paymentId());
                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    try {
                        notificationSenderService.sendPaymentSuccessEmail(order, payment);
                        notificationSenderService.sendPaymentSuccessWhatsApp(order, payment);
                    } catch (Exception e) {
                        log.error("Failed to send payment success notification: {}", e.getMessage());
                    }
                }
            }
            case PaymentEvent.PAYMENT_FAILED -> notificationService.createNotification(
                    userId,
                    NotificationType.PAYMENT_FAILED,
                    "Payment Failed",
                    "Payment for order #" + event.orderNumber() + " failed. Please try again.",
                    ReferenceType.ORDER,
                    order.getId()
            );
            case PaymentEvent.PAYMENT_EXPIRED -> notificationService.createNotification(
                    userId,
                    NotificationType.PAYMENT_EXPIRED,
                    "Payment Expired",
                    "Payment for order #" + event.orderNumber() + " has expired.",
                    ReferenceType.ORDER,
                    order.getId()
            );
        }
    }
}

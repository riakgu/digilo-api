package com.riakgu.digilo.payment;

import com.riakgu.digilo.common.exception.BadRequestException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.event.EventPublisher;
import com.riakgu.digilo.event.PaymentEvent;
import com.riakgu.digilo.order.Order;
import com.riakgu.digilo.order.OrderRepository;
import com.riakgu.digilo.order.OrderService;
import com.riakgu.digilo.order.OrderStatus;
import com.riakgu.digilo.order.dto.UpdateOrderStatusRequest;
import com.riakgu.digilo.payment.dto.CreatePaymentRequest;
import com.riakgu.digilo.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final MidtransService midtransService;
    private final EventPublisher eventPublisher;

    @Transactional
    public PaymentResponse createPayment(Long userId, CreatePaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found"));

        // Validate ownership
        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Order does not belong to you");
        }

        // Validate order status
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Order is not in pending status");
        }

        // Try to create placeholder payment FIRST
        Payment payment = Payment.builder()
                .order(order)
                .provider("MIDTRANS")
                .paymentType(request.getPaymentType())
                .providerOrderId(order.getOrderNumber())
                .amount(order.getTotalAmount())
                .currency("IDR")
                .status(PaymentStatus.PENDING)
                .expiredAt(midtransService.calculateExpiry())
                .build();

        try {
            // Try to insert - unique constraint will prevent duplicates
            payment = paymentRepository.saveAndFlush(payment);
            log.info("Payment record created for order {}: {}", order.getOrderNumber(), payment.getId());
        } catch (DataIntegrityViolationException e) {
            // Another request already created payment for this order
            log.warn("Payment already exists for order {}, fetching existing payment", order.getOrderNumber());

            // Fetch and return existing payment
            Payment existingPayment = paymentRepository.findByProviderOrderId(order.getOrderNumber())
                    .orElseThrow(() -> new BadRequestException("Payment already exists for this order"));
            return PaymentResponse.fromEntity(existingPayment);
        }

        // Call Midtrans
        Map<String, Object> chargeResponse = midtransService.createQrisCharge(
                order.getOrderNumber(),
                order.getTotalAmount().longValue()
        );

        // Extract data from Midtrans response
        String transactionId = (String) chargeResponse.get("transaction_id");
        String qrCodeUrl = extractQrCodeUrl(chargeResponse);

        // Update payment with Midtrans data
        payment.setProviderTransactionId(transactionId);
        payment.setQrCodeUrl(qrCodeUrl);
        payment.setRawChargeResponse(chargeResponse);

        payment = paymentRepository.save(payment);

        log.info("Payment updated with Midtrans data for order {}: transaction_id={}",
                order.getOrderNumber(), transactionId);

        // Publish payment created event
        eventPublisher.publishPaymentEvent(
                PaymentEvent.paymentCreated(order.getOrderNumber(), payment.getId(), order.getTotalAmount()));

        return PaymentResponse.fromEntity(payment);
    }

    @Transactional
    public void handleNotification(Map<String, Object> payload) {
        String orderId = (String) payload.get("order_id");
        String transactionStatus = (String) payload.get("transaction_status");
        String fraudStatus = (String) payload.get("fraud_status");
        String statusCode = (String) payload.get("status_code");
        String grossAmount = (String) payload.get("gross_amount");
        String signatureKey = (String) payload.get("signature_key");
        String transactionId = (String) payload.get("transaction_id");

        log.info("Received notification for order {}: status={}", orderId, transactionStatus);

        // Verify signature
        if (!midtransService.verifySignature(orderId, statusCode, grossAmount, signatureKey)) {
            log.error("Invalid signature for order {}", orderId);
            throw new BadRequestException("Invalid signature");
        }

        // Find payment
        Payment payment = paymentRepository.findByProviderOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Payment not found for order: " + orderId));

        // Store raw notification
        payment.setRawNotificationPayload(payload);

        // Update transaction ID if not set
        if (payment.getProviderTransactionId() == null && transactionId != null) {
            payment.setProviderTransactionId(transactionId);
        }

        // Map and update status
        PaymentStatus newStatus = midtransService.mapTransactionStatus(transactionStatus, fraudStatus);
        PaymentStatus oldStatus = payment.getStatus();

        if (oldStatus != newStatus) {
            processStatusChange(payment, newStatus, oldStatus);
        }

        paymentRepository.save(payment);
    }

    @Transactional
    public PaymentResponse checkStatus(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        if (!payment.getOrder().getUser().getId().equals(userId)) {
            throw new BadRequestException("Payment does not belong to you");
        }

        // Call Midtrans to get latest status
        Map<String, Object> statusResponse = midtransService.getTransactionStatus(
                payment.getProviderOrderId()
        );

        payment.setRawStatusResponse(statusResponse);

        String transactionStatus = (String) statusResponse.get("transaction_status");
        String fraudStatus = (String) statusResponse.get("fraud_status");

        PaymentStatus newStatus = midtransService.mapTransactionStatus(transactionStatus, fraudStatus);
        PaymentStatus oldStatus = payment.getStatus();

        if (oldStatus != newStatus) {
            processStatusChange(payment, newStatus, oldStatus);
        }

        paymentRepository.save(payment);

        return PaymentResponse.fromEntity(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        if (!payment.getOrder().getUser().getId().equals(userId)) {
            throw new BadRequestException("Payment does not belong to you");
        }

        return PaymentResponse.fromEntity(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(Long orderId, Long userId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Payment not found for this order"));

        if (!payment.getOrder().getUser().getId().equals(userId)) {
            throw new BadRequestException("Payment does not belong to you");
        }

        return PaymentResponse.fromEntity(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyPayments(Long userId, Pageable pageable) {
        return paymentRepository.findByOrderUserId(userId, pageable)
                .map(PaymentResponse::fromEntity);
    }

    // Admin methods
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(String orderNumber, PaymentStatus status, Pageable pageable) {
        return paymentRepository.findAllWithFilters(orderNumber, status, pageable)
                .map(PaymentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByIdAdmin(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        return PaymentResponse.fromEntity(payment);
    }

    @Transactional
    public PaymentResponse refundPayment(Long paymentId, String notes) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Can only refund SUCCESS payments");
        }

        // Update payment status
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        // Update order status to REFUNDED
        Order order = payment.getOrder();
        updateOrderStatus(order.getId(), OrderStatus.REFUNDED);

        // Add refund notes to order
        String existingNotes = order.getNotes() != null ? order.getNotes() + "\n" : "";
        order.setNotes(existingNotes + "[REFUND] " + notes);
        orderRepository.save(order);

        // Publish refund event
        eventPublisher.publishPaymentEvent(
                PaymentEvent.paymentRefunded(order.getOrderNumber(), payment.getId(), payment.getAmount()));

        log.info("Payment {} refunded for order {}: {}", paymentId, order.getOrderNumber(), notes);

        return PaymentResponse.fromEntity(payment);
    }

    @Transactional
    public PaymentResponse cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Can only cancel PENDING payments");
        }

        // Call Midtrans cancel API
        boolean cancelled = midtransService.cancelTransaction(payment.getProviderOrderId());

        if (!cancelled) {
            throw new BadRequestException("Failed to cancel payment in Midtrans");
        }

        // Update payment status
        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);

        // Update order status to CANCELLED (this also releases inventory)
        updateOrderStatus(payment.getOrder().getId(), OrderStatus.CANCELLED);

        // Publish cancel event
        eventPublisher.publishPaymentEvent(
                PaymentEvent.paymentCancelled(payment.getProviderOrderId(), payment.getId(), payment.getAmount()));

        log.info("Payment {} cancelled by admin for order {}", paymentId, payment.getOrder().getOrderNumber());

        return PaymentResponse.fromEntity(payment);
    }

    @Transactional
    public void cancelPaymentByOrder(Long orderId) {
        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
        
        if (paymentOpt.isEmpty()) {
            log.info("No payment found for order {}, nothing to cancel", orderId);
            return;
        }

        Payment payment = paymentOpt.get();
        
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("Payment {} is not PENDING (status={}), skipping cancel", 
                    payment.getId(), payment.getStatus());
            return;
        }

        // Call Midtrans cancel API
        boolean cancelled = midtransService.cancelTransaction(payment.getProviderOrderId());
        
        if (cancelled) {
            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);
            
            eventPublisher.publishPaymentEvent(
                    PaymentEvent.paymentCancelled(payment.getProviderOrderId(), payment.getId(), payment.getAmount()));
            
            log.info("Payment {} cancelled via Midtrans for order {}", payment.getId(), orderId);
        } else {
            log.warn("Failed to cancel payment {} via Midtrans, may already be expired or processed", payment.getId());
        }
    }

    private void processStatusChange(Payment payment, PaymentStatus newStatus, PaymentStatus oldStatus) {
        payment.setStatus(newStatus);
        String orderId = payment.getProviderOrderId();

        if (newStatus == PaymentStatus.SUCCESS) {
            payment.setPaidAt(Instant.now());
            updateOrderStatus(payment.getOrder().getId(), OrderStatus.PAID);
            eventPublisher.publishPaymentEvent(
                    PaymentEvent.paymentSuccess(orderId, payment.getId(), payment.getAmount()));
        } else if (newStatus == PaymentStatus.FAILED) {
            updateOrderStatus(payment.getOrder().getId(), OrderStatus.FAILED);
            eventPublisher.publishPaymentEvent(
                    PaymentEvent.paymentFailed(orderId, payment.getId(), payment.getAmount()));
        } else if (newStatus == PaymentStatus.EXPIRED) {
            updateOrderStatus(payment.getOrder().getId(), OrderStatus.FAILED);
            eventPublisher.publishPaymentEvent(
                    PaymentEvent.paymentExpired(orderId, payment.getId(), payment.getAmount()));
        } else if (newStatus == PaymentStatus.CANCELLED) {
            updateOrderStatus(payment.getOrder().getId(), OrderStatus.CANCELLED);
            eventPublisher.publishPaymentEvent(
                    PaymentEvent.paymentCancelled(orderId, payment.getId(), payment.getAmount()));
        }

        log.info("Payment {} status changed: {} -> {}", payment.getId(), oldStatus, newStatus);
    }

    private void updateOrderStatus(Long orderId, OrderStatus status) {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(status);
        orderService.updateStatus(orderId, request);
    }

    private String extractQrCodeUrl(Map<String, Object> response) {
        if (response.get("actions") instanceof List<?> actions) {
            for (Object item : actions) {
                if (item instanceof Map<?, ?> action
                        && "generate-qr-code".equals(action.get("name"))) {
                    return (String) action.get("url");
                }
            }
        }
        return null;
    }
}
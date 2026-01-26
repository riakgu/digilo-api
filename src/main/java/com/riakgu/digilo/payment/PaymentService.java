package com.riakgu.digilo.payment;

import com.riakgu.digilo.common.exception.BadRequestException;
import com.riakgu.digilo.common.exception.NotFoundException;
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
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final MidtransService midtransService;

    @Transactional
    public PaymentResponse createPayment(Long userId, CreatePaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Order does not belong to you");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Order is not in pending status");
        }

        Payment existing = paymentRepository.findByProviderOrderId(order.getOrderNumber()).orElse(null);
        if (existing != null) {
            log.info("Returning existing payment for order {}", order.getOrderNumber());
            return PaymentResponse.fromEntity(existing);
        }

        // Call Midtrans to create charge
        Map<String, Object> chargeResponse = midtransService.createQrisCharge(
                order.getOrderNumber(),
                order.getTotalAmount().longValue()
        );

        // Extract data from response
        String transactionId = (String) chargeResponse.get("transaction_id");
        String qrCodeUrl = extractQrCodeUrl(chargeResponse);

        Payment payment = Payment.builder()
                .order(order)
                .provider("MIDTRANS")
                .paymentType(request.getPaymentType())
                .providerOrderId(order.getOrderNumber())
                .providerTransactionId(transactionId)
                .amount(order.getTotalAmount())
                .currency("IDR")
                .status(PaymentStatus.PENDING)
                .qrCodeUrl(qrCodeUrl)
                .expiredAt(midtransService.calculateExpiry())
                .rawChargeResponse(chargeResponse)
                .build();

        try {
            payment = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate payment prevented for order {}", order.getOrderNumber());
            return paymentRepository.findByProviderOrderId(order.getOrderNumber())
                    .map(PaymentResponse::fromEntity)
                    .orElseThrow(() -> new IllegalStateException("Duplicate detected but payment not found"));
        }

        log.info("Payment created for order {}: {}", order.getOrderNumber(), payment.getId());
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
            payment.setStatus(newStatus);

            if (newStatus == PaymentStatus.SUCCESS) {
                payment.setPaidAt(Instant.now());
                // Update order status to PAID
                updateOrderStatus(payment.getOrder().getId(), OrderStatus.PAID);
            } else if (newStatus == PaymentStatus.FAILED || newStatus == PaymentStatus.EXPIRED) {
                // Update order status to FAILED/CANCELLED
                updateOrderStatus(payment.getOrder().getId(), OrderStatus.FAILED);
            }

            log.info("Payment {} status changed: {} -> {}", payment.getId(), oldStatus, newStatus);
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
            payment.setStatus(newStatus);

            if (newStatus == PaymentStatus.SUCCESS) {
                payment.setPaidAt(Instant.now());
                updateOrderStatus(payment.getOrder().getId(), OrderStatus.PAID);
            }

            log.info("Payment {} status updated from check: {} -> {}", payment.getId(), oldStatus, newStatus);
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
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable)
                .map(PaymentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByIdAdmin(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        return PaymentResponse.fromEntity(payment);
    }

    private void updateOrderStatus(Long orderId, OrderStatus status) {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(status);
        orderService.updateStatus(orderId, request);
    }

    private String extractQrCodeUrl(Map<String, Object> response) {
        // QRIS response has actions array with QR code URL
        if (response.containsKey("actions")) {
            var actions = (java.util.List<Map<String, Object>>) response.get("actions");
            for (Map<String, Object> action : actions) {
                if ("generate-qr-code".equals(action.get("name"))) {
                    return (String) action.get("url");
                }
            }
        }
        return null;
    }
}
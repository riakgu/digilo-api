package com.riakgu.digilo.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Scheduled job to sync stale PENDING payments with Midtrans.
 * Acts as a safety net when webhooks fail or frontend doesn't poll.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final MidtransService midtransService;

    private static final int STALE_THRESHOLD_SECONDS = 120; // 2 minutes

    /**
     * Runs every 5 minutes to sync stale PENDING payments.
     * Only processes payments older than 2 minutes to give webhooks time to arrive.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void syncStalePayments() {
        Instant threshold = Instant.now().minusSeconds(STALE_THRESHOLD_SECONDS);
        
        List<Payment> stalePayments = paymentRepository.findStalePayments(
                PaymentStatus.PENDING, 
                threshold
        );

        if (stalePayments.isEmpty()) {
            return;
        }

        log.info("Found {} stale PENDING payments to sync", stalePayments.size());

        for (Payment payment : stalePayments) {
            syncPaymentStatus(payment);
        }
    }

    private void syncPaymentStatus(Payment payment) {
        try {
            log.debug("Syncing payment {} for order {}", 
                    payment.getId(), payment.getProviderOrderId());

            // Poll Midtrans for actual status
            Map<String, Object> statusResponse = midtransService.getTransactionStatus(
                    payment.getProviderOrderId()
            );

            String transactionStatus = (String) statusResponse.get("transaction_status");
            String fraudStatus = (String) statusResponse.get("fraud_status");

            PaymentStatus newStatus = midtransService.mapTransactionStatus(transactionStatus, fraudStatus);
            PaymentStatus oldStatus = payment.getStatus();

            if (oldStatus != newStatus) {
                log.info("Payment {} status changed via scheduled sync: {} -> {}", 
                        payment.getId(), oldStatus, newStatus);
                
                // Use the same notification handling logic
                paymentService.handleNotification(statusResponse);
            }

        } catch (Exception e) {
            log.warn("Failed to sync payment {} for order {}: {}", 
                    payment.getId(), payment.getProviderOrderId(), e.getMessage());
            // Don't rethrow - continue with other payments
        }
    }
}

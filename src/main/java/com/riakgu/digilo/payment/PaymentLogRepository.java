package com.riakgu.digilo.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {

    List<PaymentLog> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);
}

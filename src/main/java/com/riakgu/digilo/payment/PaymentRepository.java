package com.riakgu.digilo.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByProviderOrderId(String providerOrderId);

    Optional<Payment> findByProviderTransactionId(String providerTransactionId);

    Optional<Payment> findByOrderId(Long orderId);

    Page<Payment> findByOrderUserId(Long userId, Pageable pageable);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    boolean existsByProviderOrderId(String providerOrderId);
}
package com.riakgu.digilo.payment;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByProviderOrderId(String providerOrderId);

    Optional<Payment> findByProviderTransactionId(String providerTransactionId);

    Optional<Payment> findByOrderId(Long orderId);

    Page<Payment> findByOrderUserId(Long userId, Pageable pageable);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    boolean existsByProviderOrderId(String providerOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.providerOrderId = :providerOrderId")
    Optional<Payment> findByProviderOrderIdWithLock(@Param("providerOrderId") String providerOrderId);

    @Query("SELECT p FROM Payment p WHERE " +
            "(:providerOrderId IS NULL OR p.providerOrderId LIKE %:providerOrderId%) " +
            "AND (:status IS NULL OR p.status = :status)")
    Page<Payment> findAllWithFilters(
            @Param("providerOrderId") String providerOrderId,
            @Param("status") PaymentStatus status,
            Pageable pageable
    );
}
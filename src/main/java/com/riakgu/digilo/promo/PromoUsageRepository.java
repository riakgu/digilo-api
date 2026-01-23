package com.riakgu.digilo.promo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromoUsageRepository extends JpaRepository<PromoUsage, Long> {

    long countByPromoId(Long promoId);

    long countByPromoIdAndUserId(Long promoId, Long userId);

    boolean existsByPromoIdAndUserId(Long promoId, Long userId);

    boolean existsByOrderId(Long orderId);

    Optional<PromoUsage> findByOrderId(Long orderId);
}
package com.riakgu.digilo.promo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PromoUsageRepository extends JpaRepository<PromoUsage, Long> {

    long countByPromoId(Long promoId);

    long countByPromoIdAndUserId(Long promoId, Long userId);

    boolean existsByPromoIdAndUserId(Long promoId, Long userId);

    boolean existsByOrderId(Long orderId);

    Optional<PromoUsage> findByOrderId(Long orderId);

    @Query("SELECT pu.promo.id, COUNT(pu) FROM PromoUsage pu WHERE pu.promo.id IN :promoIds GROUP BY pu.promo.id")
    List<Object[]> countByPromoIds(@Param("promoIds") List<Long> promoIds);
}

package com.riakgu.digilo.promo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PromoRepository extends JpaRepository<Promo, Long> {

    Optional<Promo> findByCode(String code);

    Optional<Promo> findByCodeAndIsActive(String code, Boolean isActive);

    Page<Promo> findByIsActive(Boolean isActive, Pageable pageable);

    boolean existsByCode(String code);

    @Query("SELECT p FROM Promo p WHERE " +
            "(:code IS NULL OR p.code LIKE %:code%) " +
            "AND (:isActive IS NULL OR p.isActive = :isActive) " +
            "AND (:discountType IS NULL OR p.discountType = :discountType)")
    Page<Promo> findAllWithFilters(
            @Param("code") String code,
            @Param("isActive") Boolean isActive,
            @Param("discountType") DiscountType discountType,
            Pageable pageable
    );
}
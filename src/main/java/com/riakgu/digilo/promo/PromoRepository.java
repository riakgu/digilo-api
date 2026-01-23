package com.riakgu.digilo.promo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromoRepository extends JpaRepository<Promo, Long> {

    Optional<Promo> findByCode(String code);

    Optional<Promo> findByCodeAndIsActive(String code, Boolean isActive);

    Page<Promo> findByIsActive(Boolean isActive, Pageable pageable);

    boolean existsByCode(String code);
}
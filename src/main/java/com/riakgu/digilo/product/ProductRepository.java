package com.riakgu.digilo.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product,Long> {

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    Optional<Product> findBySlug(String slug);

    Optional<Product> findById(Long id);

    Optional<Product> findBySlugAndIsActive(String slug, Boolean isActive);

    Page<Product> findAllByIsActive(Boolean isActive, Pageable pageable);

    Page<Product> findAll(Pageable pageable);

    Page<Product> findAllByCategoriesCategorySlugAndIsActive(String categorySlug, Boolean isActive, Pageable pageable);

}

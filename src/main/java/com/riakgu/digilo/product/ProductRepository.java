package com.riakgu.digilo.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);

}

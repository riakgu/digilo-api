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

    // Sort by latest (createdAt DESC)
    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.createdAt DESC")
    Page<Product> findAllActiveOrderByLatest(Pageable pageable);

    // Sort by min price ASC
    @Query("SELECT p FROM Product p LEFT JOIN p.variants v ON v.isActive = true WHERE p.isActive = true GROUP BY p ORDER BY MIN(v.price) ASC")
    Page<Product> findAllActiveOrderByPriceAsc(Pageable pageable);

    // Sort by min price DESC  
    @Query("SELECT p FROM Product p LEFT JOIN p.variants v ON v.isActive = true WHERE p.isActive = true GROUP BY p ORDER BY MIN(v.price) DESC")
    Page<Product> findAllActiveOrderByPriceDesc(Pageable pageable);

    // Sort by trending (order count DESC)
    @Query(value = "SELECT p.* FROM products p " +
            "LEFT JOIN product_variants v ON v.product_id = p.id " +
            "LEFT JOIN order_items oi ON oi.variant_id = v.id " +
            "LEFT JOIN orders o ON o.id = oi.order_id AND o.status IN ('PAID', 'COMPLETED') " +
            "WHERE p.is_active = true " +
            "GROUP BY p.id " +
            "ORDER BY COALESCE(SUM(oi.quantity), 0) DESC",
            countQuery = "SELECT COUNT(DISTINCT p.id) FROM products p WHERE p.is_active = true",
            nativeQuery = true)
    Page<Product> findAllActiveOrderByTrending(Pageable pageable);
}


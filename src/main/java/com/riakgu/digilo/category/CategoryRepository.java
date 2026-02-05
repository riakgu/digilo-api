package com.riakgu.digilo.category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    Optional<Category> findBySlug(String slug);

    Optional<Category> findById(Long id);

    Optional<Category> findBySlugAndIsActive(String slug, Boolean isActive);

    Page<Category> findAllByIsActive(Boolean isActive, Pageable pageable);

    Page<Category> findAll(Pageable pageable);

    Page<Category> findAllByProductsProductSlugAndIsActive(String productSlug, Boolean isActive, Pageable pageable);

    @Query("SELECT c FROM Category c WHERE " +
            "(:search IS NULL OR :search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "OR LOWER(c.slug) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
            "AND (:isActive IS NULL OR c.isActive = :isActive)")
    Page<Category> findAllWithFilters(
            @Param("search") String search,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}


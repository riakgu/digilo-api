package com.riakgu.digilo.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    Optional<Category> findBySlug(String slug);

    Optional<Category> findById(Long id);

    Optional<Category> findBySlugAndIsActive(String slug, Boolean isActive);
}

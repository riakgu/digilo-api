package com.riakgu.digilo.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductInventoryRepository extends JpaRepository<ProductInventory, Long> {

    List<ProductInventory> findByVariantId(Long variantId);

    List<ProductInventory> findByVariantIdAndStatus(Long variantId, InventoryStatus status);

    long countByVariantIdAndStatus(Long variantId, InventoryStatus status);

    Optional<ProductInventory> findFirstByVariantIdAndStatus(Long variantId, InventoryStatus status);
}
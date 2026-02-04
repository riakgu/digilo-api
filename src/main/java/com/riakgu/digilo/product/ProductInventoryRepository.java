package com.riakgu.digilo.product;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductInventoryRepository extends JpaRepository<ProductInventory, Long> {

    List<ProductInventory> findByVariantId(Long variantId);

    List<ProductInventory> findByVariantIdAndStatus(Long variantId, InventoryStatus status);

    long countByVariantIdAndStatus(Long variantId, InventoryStatus status);

    Optional<ProductInventory> findFirstByVariantIdAndStatus(Long variantId, InventoryStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pi FROM ProductInventory pi WHERE pi.variant.id = :variantId AND pi.status = 'AVAILABLE' ORDER BY pi.id ASC")
    List<ProductInventory> findAvailableForUpdate(
            @Param("variantId") Long variantId,
            Pageable pageable
    );

    List<ProductInventory> findByOrderItemIdAndStatus(Long orderItemId, InventoryStatus status);

    @Query("SELECT pi.variant.id, COUNT(pi) FROM ProductInventory pi " +
           "WHERE pi.variant.id IN :variantIds AND pi.status = :status " +
           "GROUP BY pi.variant.id")
    List<Object[]> countByVariantIdsAndStatus(
            @Param("variantIds") List<Long> variantIds,
            @Param("status") InventoryStatus status);

    long countByOrderItemIdAndStatus(Long orderItemId, InventoryStatus status);

    @Query("SELECT pi FROM ProductInventory pi WHERE " +
            "(:variantId IS NULL OR pi.variant.id = :variantId) " +
            "AND (:status IS NULL OR pi.status = :status)")
    Page<ProductInventory> findAllWithFilters(
            @Param("variantId") Long variantId,
            @Param("status") InventoryStatus status,
            Pageable pageable
    );
}
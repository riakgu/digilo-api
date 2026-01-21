package com.riakgu.digilo.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findAllByProductId(Long productId);

    Optional<ProductImage> findByProductIdAndIsPrimary(Long productId, Boolean isPrimary);

}

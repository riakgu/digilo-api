package com.riakgu.digilo.cart;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    Optional<CartItem> findByCartIdAndVariantId(Long cartId, Long variantId);

    void deleteByCartIdAndVariantId(Long cartId, Long variantId);
}
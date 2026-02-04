package com.riakgu.digilo.cart;

import com.riakgu.digilo.cart.dto.*;
import com.riakgu.digilo.common.exception.BadRequestException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.product.*;
import com.riakgu.digilo.product.dto.ProductVariantResponse;
import com.riakgu.digilo.user.User;
import com.riakgu.digilo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductInventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final ProductImageHelper productImageHelper;

    @Transactional
    public CartResponse getOrCreateCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createCart(userId));

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createCart(userId));

        ProductVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new NotFoundException("Variant not found"));

        if (!variant.getIsActive()) {
            throw new BadRequestException("Variant is not available");
        }

        // Check existing quantity in cart
        CartItem existingItem = cartItemRepository
                .findByCartIdAndVariantId(cart.getId(), variant.getId())
                .orElse(null);

        int existingQuantity = existingItem != null ? existingItem.getQuantity() : 0;
        int totalQuantity = existingQuantity + request.getQuantity();

        // Validate stock for AUTO delivery type
        if (variant.getDeliveryType() == DeliveryType.AUTO) {
            long availableStock = inventoryRepository.countByVariantIdAndStatus(
                    variant.getId(), InventoryStatus.AVAILABLE);
            if (availableStock < totalQuantity) {
                throw new BadRequestException("Not enough stock available");
            }
        }

        if (existingItem != null) {
            existingItem.setQuantity(totalQuantity);
            cartItemRepository.save(existingItem);
            log.info("Cart item quantity updated: userId={}, variantId={}, newQuantity={}", 
                    userId, variant.getId(), totalQuantity);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
            log.info("Item added to cart: userId={}, variantId={}, quantity={}", 
                    userId, variant.getId(), request.getQuantity());
        }

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse updateCartItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Cart not found"));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Item does not belong to your cart");
        }

        if (item.getVariant().getDeliveryType() == DeliveryType.AUTO) {
            long availableStock = inventoryRepository.countByVariantIdAndStatus(
                    item.getVariant().getId(), InventoryStatus.AVAILABLE);
            if (availableStock < request.getQuantity()) {
                throw new BadRequestException("Not enough stock available");
            }
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse removeFromCart(Long userId, Long itemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Cart not found"));

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Item does not belong to your cart");
        }

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        log.info("Item removed from cart: userId={}, itemId={}", userId, itemId);

        return buildCartResponse(cart);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Cart not found"));

        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Cart cleared: userId={}", userId);
    }

    private Cart createCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Cart cart = Cart.builder()
                .user(user)
                .build();

        return cartRepository.save(cart);
    }

    private CartResponse buildCartResponse(Cart cart) {
        if (cart.getItems().isEmpty()) {
            return CartResponse.fromItems(cart.getId(), List.of());
        }

        // Batch fetch stock counts to avoid N+1 queries
        List<Long> variantIds = cart.getItems().stream()
                .map(item -> item.getVariant().getId())
                .collect(Collectors.toList());

        Map<Long, Long> stockMap = inventoryRepository
                .countByVariantIdsAndStatus(variantIds, InventoryStatus.AVAILABLE)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> {
                    long stock = stockMap.getOrDefault(item.getVariant().getId(), 0L);
                    ProductVariantResponse variantResponse =
                            ProductVariantResponse.fromEntity(item.getVariant(), stock, productImageHelper.getDisplayImageUrl(item.getVariant().getProduct()));
                    return CartItemResponse.fromEntity(item, variantResponse);
                })
                .collect(Collectors.toList());

        return CartResponse.fromItems(cart.getId(), items);
    }
}
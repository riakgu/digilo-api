package com.riakgu.digilo.cart;

import com.riakgu.digilo.cart.dto.*;
import com.riakgu.digilo.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal Long userId
    ) {
        CartResponse cart = cartService.getOrCreateCart(userId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Cart retrieved", cart));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AddToCartRequest request
    ) {
        CartResponse cart = cartService.addToCart(userId, request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Item added to cart", cart));
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        CartResponse cart = cartService.updateCartItem(userId, itemId, request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Cart item updated", cart));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long itemId
    ) {
        CartResponse cart = cartService.removeFromCart(userId, itemId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Item removed from cart", cart));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal Long userId
    ) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Cart cleared"));
    }
}
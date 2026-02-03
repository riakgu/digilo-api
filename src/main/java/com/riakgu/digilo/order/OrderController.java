package com.riakgu.digilo.order;

import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.order.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/user/orders")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal Long userId,
            @RequestBody(required = false) CreateOrderRequest request
    ) {
        OrderResponse order = orderService.createFromCart(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("CREATED", "Order created successfully", order));
    }

    @GetMapping("/user/orders")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String orderNumber,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<OrderResponse> orders = orderService.getMyOrders(userId, orderNumber, pageable);
        return ResponseEntity.ok(ApiResponse.success("OK", "Orders retrieved", orders));
    }

    @GetMapping("/user/orders/{orderId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orderId
    ) {
        OrderResponse order = orderService.getById(orderId, userId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Order retrieved", order));
    }

    @GetMapping("/user/orders/{orderId}/credentials")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderCredentialResponse>>> getOrderCredentials(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orderId
    ) {
        List<OrderCredentialResponse> credentials = orderService.getOrderCredentials(orderId, userId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Credentials retrieved", credentials));
    }

    @PostMapping("/user/orders/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orderId
    ) {
        OrderResponse order = orderService.cancelOrder(orderId, userId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Order cancelled successfully", order));
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<OrderResponse> orders = orderService.getAllOrders(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("OK", "Orders retrieved", orders));
    }

    @GetMapping("/admin/orders/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderAdmin(
            @PathVariable Long orderId
    ) {
        OrderResponse order = orderService.getByIdAdmin(orderId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Order retrieved", order));
    }

    @GetMapping("/admin/orders/{orderId}/credentials")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderCredentialResponse>>> getOrderCredentialsAdmin(
            @PathVariable Long orderId
    ) {
        List<OrderCredentialResponse> credentials = orderService.getOrderCredentialsAdmin(orderId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Credentials retrieved", credentials));
    }

    @PatchMapping("/admin/orders/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        OrderResponse order = orderService.updateStatus(orderId, request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Order status updated", order));
    }

    @PostMapping("/admin/orders/{orderId}/items/{itemId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderCredentialResponse>> assignCredential(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody AssignCredentialRequest request
    ) {
        OrderCredentialResponse credential = orderService.assignCredential(orderId, itemId, request.getInventoryId());
        return ResponseEntity.ok(ApiResponse.success("OK", "Credential assigned successfully", credential));
    }
}
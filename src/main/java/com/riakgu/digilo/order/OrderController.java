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
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<OrderResponse> orders = orderService.getMyOrders(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("OK", "Orders retrieved", orders));
    }

    @GetMapping("/user/orders/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orderId
    ) {
        OrderResponse order = orderService.getById(orderId, userId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Order retrieved", order));
    }

    @GetMapping("/user/orders/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @AuthenticationPrincipal Long userId,
            @PathVariable String orderNumber
    ) {
        OrderResponse order = orderService.getByOrderNumber(orderNumber, userId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Order retrieved", order));
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<OrderResponse> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success("OK", "Orders retrieved", orders));
    }

    @GetMapping("/admin/orders/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<OrderResponse> orders = orderService.getOrdersByStatus(status, pageable);
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

    @PatchMapping("/admin/orders/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        OrderResponse order = orderService.updateStatus(orderId, request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Order status updated", order));
    }
}
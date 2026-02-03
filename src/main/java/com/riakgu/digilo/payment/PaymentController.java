package com.riakgu.digilo.payment;

import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.payment.dto.CreatePaymentRequest;
import com.riakgu.digilo.payment.dto.PaymentResponse;
import com.riakgu.digilo.payment.dto.RefundPaymentRequest;
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
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/user/payments")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponse payment = paymentService.createPayment(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("CREATED", "Payment created", payment));
    }

    @GetMapping("/user/payments")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getMyPayments(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<PaymentResponse> payments = paymentService.getMyPayments(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("OK", "Payments retrieved", payments));
    }

    @GetMapping("/user/payments/{paymentId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long paymentId
    ) {
        PaymentResponse payment = paymentService.getById(paymentId, userId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Payment retrieved", payment));
    }

    @GetMapping("/user/orders/{orderId}/payment")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long orderId
    ) {
        PaymentResponse payment = paymentService.getByOrderId(orderId, userId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Payment retrieved", payment));
    }

    @PostMapping("/user/payments/{paymentId}/sync")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> syncPaymentStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long paymentId
    ) {
        PaymentResponse payment = paymentService.checkStatus(paymentId, userId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Payment status synced", payment));
    }

    @PostMapping("/public/payments/notification")
    public ResponseEntity<String> handleNotification(
            @RequestBody Map<String, Object> payload
    ) {
        paymentService.handleNotification(payload);
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/admin/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getAllPayments(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<PaymentResponse> payments = paymentService.getAllPayments(pageable);
        return ResponseEntity.ok(ApiResponse.success("OK", "Payments retrieved", payments));
    }

    @GetMapping("/admin/payments/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentAdmin(
            @PathVariable Long paymentId
    ) {
        PaymentResponse payment = paymentService.getByIdAdmin(paymentId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Payment retrieved", payment));
    }

    @PostMapping("/admin/payments/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody RefundPaymentRequest request
    ) {
        PaymentResponse payment = paymentService.refundPayment(paymentId, request.getNotes());
        return ResponseEntity.ok(ApiResponse.success("OK", "Payment refunded successfully", payment));
    }
}
package com.riakgu.digilo.verification;

import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.user.dto.UserResponse;
import com.riakgu.digilo.verification.dto.VerifyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    // ==================== EMAIL ====================

    @PostMapping("/email")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> requestEmailVerification(
            @AuthenticationPrincipal Long userId
    ) {
        verificationService.sendEmailOtp(userId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Verification code sent to email"));
    }

    @PutMapping("/email")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> verifyEmail(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody VerifyRequest request
    ) {
        UserResponse user = verificationService.verifyEmail(userId, request.getOtp());
        return ResponseEntity.ok(ApiResponse.success("OK", "Email verified successfully", user));
    }

    // ==================== PHONE ====================

    @PostMapping("/phone")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> requestPhoneVerification(
            @AuthenticationPrincipal Long userId
    ) {
        verificationService.sendPhoneOtp(userId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Verification code sent to WhatsApp"));
    }

    @PutMapping("/phone")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> verifyPhone(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody VerifyRequest request
    ) {
        UserResponse user = verificationService.verifyPhone(userId, request.getOtp());
        return ResponseEntity.ok(ApiResponse.success("OK", "Phone verified successfully", user));
    }
}

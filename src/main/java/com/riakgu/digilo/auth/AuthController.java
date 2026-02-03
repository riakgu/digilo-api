package com.riakgu.digilo.auth;

import com.riakgu.digilo.auth.dto.*;
import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final VerificationService verificationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse register = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("CREATED", "Register successful", register));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse login = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Login successful", login));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse response = googleAuthService.authenticate(request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Google login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse refresh = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Refresh token successful", refresh));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.success("OK", "Logged out successfully"));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Password reset code sent to email"));
    }

    @PostMapping("/password/verify")
    public ResponseEntity<ApiResponse<ResetTokenResponse>> verifyResetOtp(@Valid @RequestBody VerifyResetOtpRequest request) {
        ResetTokenResponse response = authService.verifyResetOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OK", "OTP verified", response));
    }

    @PutMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Password reset successful"));
    }

    @PostMapping("/email/send-otp")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sendEmailOtp(@AuthenticationPrincipal Long userId) {
        verificationService.sendEmailOtp(userId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Verification code sent to email"));
    }

    @PostMapping("/email/verify")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> verifyEmail(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody VerifyRequest request
    ) {
        UserResponse user = verificationService.verifyEmail(userId, request.getOtp());
        return ResponseEntity.ok(ApiResponse.success("OK", "Email verified successfully", user));
    }

    @PostMapping("/phone/send-otp")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sendPhoneOtp(@AuthenticationPrincipal Long userId) {
        verificationService.sendPhoneOtp(userId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Verification code sent to WhatsApp"));
    }

    @PostMapping("/phone/verify")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> verifyPhone(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody VerifyRequest request
    ) {
        UserResponse user = verificationService.verifyPhone(userId, request.getOtp());
        return ResponseEntity.ok(ApiResponse.success("OK", "Phone verified successfully", user));
    }
}

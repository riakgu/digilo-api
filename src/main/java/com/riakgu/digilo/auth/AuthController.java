package com.riakgu.digilo.auth;

import com.riakgu.digilo.auth.dto.*;
import com.riakgu.digilo.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

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
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.success("OK", "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(@RequestHeader("Authorization") String authHeader) {
        authService.logoutAll(authHeader);
        return ResponseEntity.ok(ApiResponse.success("OK", "All sessions revoked"));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessions(
            @RequestHeader("Authorization") String authHeader) {
        List<SessionResponse> sessions = authService.getSessions(authHeader);
        return ResponseEntity.ok(ApiResponse.success("OK", "Active sessions retrieved", sessions));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String sessionId) {
        authService.revokeSession(authHeader, sessionId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Session revoked"));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Password reset code sent to email"));
    }

    @PostMapping("/password/verify")
    public ResponseEntity<ApiResponse<ResetTokenResponse>> verifyResetOtp(
            @Valid @RequestBody VerifyResetOtpRequest request) {
        ResetTokenResponse response = authService.verifyResetOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OK", "OTP verified", response));
    }

    @PutMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Password reset successful"));
    }
}

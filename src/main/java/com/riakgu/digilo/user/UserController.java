package com.riakgu.digilo.user;

import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.user.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserVerificationService verificationService;

    @GetMapping("/user/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal Long userId) {
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Get current user successful", user));
    }

    @PatchMapping("/user/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserResponse user = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Update profile successful", user));
    }

    @PatchMapping("/user/password")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> changePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        UserResponse user = userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Change password successful", user));
    }

    @PostMapping("/user/verify/email/send")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sendEmailOtp(@AuthenticationPrincipal Long userId) {
        verificationService.sendEmailOtp(userId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Verification code sent to email"));
    }

    @PostMapping("/user/verify/email")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> verifyEmail(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        UserResponse user = verificationService.verifyEmail(userId, request.getOtp());
        return ResponseEntity.ok(ApiResponse.success("OK", "Email verified successfully", user));
    }

    @PostMapping("/user/verify/phone/send")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sendPhoneOtp(@AuthenticationPrincipal Long userId) {
        verificationService.sendPhoneOtp(userId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Verification code sent to WhatsApp"));
    }

    @PostMapping("/user/verify/phone")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> verifyPhone(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        UserResponse user = verificationService.verifyPhone(userId, request.getOtp());
        return ResponseEntity.ok(ApiResponse.success("OK", "Phone verified successfully", user));
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<UserResponse> users = userService.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.success("OK", "Get all users successful", users));
    }

    @GetMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long id
    ) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("OK", "Get user successful", user));
    }

    @PatchMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> adminUpdate(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request
    ) {
        UserResponse user = userService.adminUpdate(id, request);
        return ResponseEntity.ok(ApiResponse.success("OK", "User updated successfully", user));
    }
}

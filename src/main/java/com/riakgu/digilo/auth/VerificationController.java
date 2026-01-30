package com.riakgu.digilo.auth;

import com.riakgu.digilo.auth.dto.SendVerificationRequest;
import com.riakgu.digilo.auth.dto.VerifyEmailRequest;
import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.user.User;
import com.riakgu.digilo.user.UserRepository;
import com.riakgu.digilo.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class VerificationController {

    private final OtpService otpService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @PostMapping("/send-verification")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sendVerification(
            @AuthenticationPrincipal Long userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return ResponseEntity.ok(ApiResponse.success("OK", "Email already verified"));
        }

        String otp = otpService.generateAndSaveOtp(user.getEmail());
        emailService.sendVerificationEmail(user.getEmail(), otp);

        return ResponseEntity.ok(ApiResponse.success("OK", "Verification email sent"));
    }

    @PostMapping("/verify-email")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> verifyEmail(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.getEmail().equals(request.getEmail())) {
            throw new NotFoundException("Email does not match");
        }

        otpService.validateOtp(request.getEmail(), request.getOtp());

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(Instant.now());
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("OK", "Email verified successfully", UserResponse.fromEntity(user)));
    }
}

package com.riakgu.digilo.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.riakgu.digilo.auth.dto.*;
import com.riakgu.digilo.common.exception.BadRequestException;
import com.riakgu.digilo.common.exception.DuplicateResourceException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.common.exception.UnauthorizedException;
import com.riakgu.digilo.common.service.OtpService;
import com.riakgu.digilo.notification.NotificationSenderService;
import com.riakgu.digilo.user.Role;
import com.riakgu.digilo.user.User;
import com.riakgu.digilo.user.UserRepository;
import com.riakgu.digilo.user.UserStatus;
import com.riakgu.digilo.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final NotificationSenderService notificationSender;
    private final StringRedisTemplate redisTemplate;

    private static final String RESET_TOKEN_PREFIX = "password:reset:";
    private static final Duration RESET_TOKEN_EXPIRY = Duration.ofMinutes(10);

    @Transactional
    public AuthResponse register(RegisterRequest request, String userAgent, String ip) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Phone number already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(false)
                .build();

        userRepository.save(user);

        log.info("User registered: email={}", user.getEmail());

        String sessionId = UUID.randomUUID().toString();
        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .accessToken(jwtService.generateAccessToken(user.getId(), user.getRole().name(), sessionId))
                .refreshToken(jwtService.generateRefreshToken(user.getId(), user.getRole().name(), sessionId, userAgent, ip))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, String userAgent, String ip) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.getPassword() == null) {
            throw new UnauthorizedException("Please use Google to login");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: invalid password for email={}", request.getEmail());
            throw new UnauthorizedException("Invalid credentials");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            log.warn("Login failed: account suspended for email={}", request.getEmail());
            throw new UnauthorizedException("Your account has been suspended");
        }

        log.info("User logged in: userId={}, email={}", user.getId(), user.getEmail());

        String sessionId = UUID.randomUUID().toString();
        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .accessToken(jwtService.generateAccessToken(user.getId(), user.getRole().name(), sessionId))
                .refreshToken(jwtService.generateRefreshToken(user.getId(), user.getRole().name(), sessionId, userAgent, ip))
                .build();

    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request, String userAgent, String ip) {
        JwtService.RefreshResult result = jwtService.refreshTokens(request.getRefreshToken(), userAgent, ip);

        User user = userRepository.findById(result.userId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        log.info("Token refreshed: userId={}, sessionId={}", result.userId(), result.sessionId());

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .accessToken(result.accessToken())
                .refreshToken(result.refreshToken())
                .build();
    }

    public void logout(String authHeader) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtService.isBlacklisted(token)) {
                throw new UnauthorizedException("Invalid token");
            }

            DecodedJWT decoded = jwtService.verifyAccessToken(token);

            Long userId = Long.valueOf(decoded.getSubject());
            String sessionId = decoded.getClaim("sid").asString();

            jwtService.blacklistAccessToken(token);

            if (sessionId != null) {
                jwtService.revokeSession(userId, sessionId);
            } else {
                // Fallback for old tokens without sessionId
                jwtService.revokeUserTokens(userId);
            }

            log.info("User logged out: userId={}, sessionId={}", userId, sessionId);
        }
    }

    public void logoutAll(String authHeader) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtService.isBlacklisted(token)) {
                throw new UnauthorizedException("Invalid token");
            }

            DecodedJWT decoded = jwtService.verifyAccessToken(token);
            Long userId = Long.valueOf(decoded.getSubject());

            jwtService.blacklistAccessToken(token);
            jwtService.revokeUserTokens(userId);

            log.info("All sessions revoked: userId={}", userId);
        }
    }

    public List<SessionResponse> getSessions(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid token");
        }

        String token = authHeader.substring(7);

        if (jwtService.isBlacklisted(token)) {
            throw new UnauthorizedException("Invalid token");
        }

        DecodedJWT decoded = jwtService.verifyAccessToken(token);
        Long userId = Long.valueOf(decoded.getSubject());
        String currentSessionId = decoded.getClaim("sid").asString();

        return jwtService.getActiveSessions(userId, currentSessionId);
    }

    public void revokeSession(String authHeader, String sessionId) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid token");
        }

        String token = authHeader.substring(7);

        if (jwtService.isBlacklisted(token)) {
            throw new UnauthorizedException("Invalid token");
        }

        DecodedJWT decoded = jwtService.verifyAccessToken(token);
        Long userId = Long.valueOf(decoded.getSubject());
        String currentSessionId = decoded.getClaim("sid").asString();

        if (sessionId.equals(currentSessionId)) {
            throw new BadRequestException("Cannot revoke current session. Use /logout instead.");
        }

        jwtService.revokeSession(userId, sessionId);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String otp = otpService.generateAndSaveOtp("password:" + user.getEmail());
            notificationSender.sendPasswordResetEmail(user.getName(), user.getEmail(), otp);
            log.info("Password reset OTP sent to email={}", request.getEmail());
        });
    }

    public ResetTokenResponse verifyResetOtp(VerifyResetOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found"));

        otpService.validateOtp("password:" + user.getEmail(), request.getOtp());

        String resetToken = UUID.randomUUID().toString();
        String tokenKey = RESET_TOKEN_PREFIX + resetToken;
        redisTemplate.opsForValue().set(tokenKey, user.getId().toString(), RESET_TOKEN_EXPIRY);

        log.info("Password reset OTP verified for email={}", request.getEmail());
        return new ResetTokenResponse(resetToken);
    }

    public void resetPassword(ResetPasswordRequest request) {
        String tokenKey = RESET_TOKEN_PREFIX + request.getToken();
        String userIdStr = redisTemplate.opsForValue().get(tokenKey);

        if (userIdStr == null) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        Long userId = Long.valueOf(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        redisTemplate.delete(tokenKey);

        // Revoke ALL sessions on password reset
        jwtService.revokeUserTokens(userId);

        log.info("Password reset successful for userId={}", userId);
    }
}

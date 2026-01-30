package com.riakgu.digilo.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.riakgu.digilo.auth.dto.AuthResponse;
import com.riakgu.digilo.auth.dto.LoginRequest;
import com.riakgu.digilo.auth.dto.RefreshRequest;
import com.riakgu.digilo.auth.dto.RegisterRequest;
import com.riakgu.digilo.common.exception.DuplicateResourceException;
import com.riakgu.digilo.common.exception.UnauthorizedException;
import com.riakgu.digilo.user.Role;
import com.riakgu.digilo.user.User;
import com.riakgu.digilo.user.UserRepository;
import com.riakgu.digilo.user.UserStatus;
import com.riakgu.digilo.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        log.info("User registered: email={}", user.getEmail());

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .accessToken(jwtService.generateAccessToken(user.getId(), user.getRole().name()))
                .refreshToken(jwtService.generateRefreshToken(user.getId()))
                .build();
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: invalid password for email={}", request.getEmail());
            throw new UnauthorizedException("Invalid credentials");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            log.warn("Login failed: account suspended for email={}", request.getEmail());
            throw new UnauthorizedException("Your account has been suspended");
        }

        log.info("User logged in: userId={}, email={}", user.getId(), user.getEmail());

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .accessToken(jwtService.generateAccessToken(user.getId(), user.getRole().name()))
                .refreshToken(jwtService.generateRefreshToken(user.getId()))
                .build();

    }

    public AuthResponse refresh(RefreshRequest request) {

        return AuthResponse.builder()
                .accessToken(jwtService.refreshAccessToken(request.getRefreshToken()))
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

            jwtService.blacklistAccessToken(token);
            jwtService.revokeUserTokens(userId);

            log.info("User logged out: userId={}", userId);
        }
    }

}

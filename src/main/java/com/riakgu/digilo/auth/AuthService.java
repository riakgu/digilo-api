package com.riakgu.digilo.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.riakgu.digilo.auth.dto.AuthResponse;
import com.riakgu.digilo.auth.dto.LoginRequest;
import com.riakgu.digilo.auth.dto.RefreshRequest;
import com.riakgu.digilo.auth.dto.RegisterRequest;
import com.riakgu.digilo.user.Role;
import com.riakgu.digilo.user.User;
import com.riakgu.digilo.user.UserRepository;
import com.riakgu.digilo.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.PublicKey;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

    }

    public AuthResponse refresh(RefreshRequest request) {

        DecodedJWT decoded = jwtService.verifyRefreshToken(request.getRefreshToken());

        Long userId = Long.valueOf(decoded.getSubject());

        String accessToken = jwtService.generateAccessToken(userId);

        return AuthResponse.refresh(accessToken);

    }

}

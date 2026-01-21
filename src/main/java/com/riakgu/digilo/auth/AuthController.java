package com.riakgu.digilo.auth;

import com.riakgu.digilo.auth.dto.AuthResponse;
import com.riakgu.digilo.auth.dto.LoginRequest;
import com.riakgu.digilo.auth.dto.RefreshRequest;
import com.riakgu.digilo.auth.dto.RegisterRequest;
import com.riakgu.digilo.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse register = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("CREATED", "Register successful", register));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse login = authService.login(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Login successful", login));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse refresh = authService.refresh(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Refresh token successful", refresh));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Logged out successfully"));
    }

}

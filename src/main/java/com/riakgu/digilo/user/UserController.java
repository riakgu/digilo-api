package com.riakgu.digilo.user;

import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.user.dto.ChangePasswordRequest;
import com.riakgu.digilo.user.dto.UpdateProfileRequest;
import com.riakgu.digilo.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal Long userId) {
        UserResponse user = userService.getCurrentUser(userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Get current user successful", user));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserResponse user = userService.updateProfile(userId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("UPDATED", "Update profile successful", user));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<UserResponse>> changePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
       UserResponse user = userService.changePassword(userId, request);

       return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("UPDATED", "Change password successful", user));
    }
}

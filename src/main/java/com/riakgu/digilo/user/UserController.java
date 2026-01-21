package com.riakgu.digilo.user;

import com.riakgu.digilo.category.dto.CategoryResponse;
import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.user.dto.ChangePasswordRequest;
import com.riakgu.digilo.user.dto.UpdateProfileRequest;
import com.riakgu.digilo.user.dto.UpdateRoleRequest;
import com.riakgu.digilo.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/user/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal Long userId) {
        UserResponse user = userService.getProfile(userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Get current user successful", user));
    }

    @PatchMapping("/user/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserResponse user = userService.updateProfile(userId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("UPDATED", "Update profile successful", user));
    }

    @PatchMapping("/user/password")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> changePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
       UserResponse user = userService.changePassword(userId, request);

       return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("UPDATED", "Change password successful", user));
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<UserResponse> users = userService.getAll(pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Get all users successful", users));
    }

    @PatchMapping("/admin/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        UserResponse user = userService.updateRole(id, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("UPDATED", "Change role successful", user));
    }
}

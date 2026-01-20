package com.riakgu.digilo.user;


import com.riakgu.digilo.auth.dto.AuthResponse;
import com.riakgu.digilo.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> me(@AuthenticationPrincipal Long userId) {
        AuthResponse me = userService.me(userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(me));
    }
}

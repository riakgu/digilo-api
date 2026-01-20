package com.riakgu.digilo.user;

import com.riakgu.digilo.auth.dto.AuthResponse;
import com.riakgu.digilo.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public AuthResponse me(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .build();
    }

}

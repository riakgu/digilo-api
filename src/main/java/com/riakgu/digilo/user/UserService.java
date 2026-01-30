package com.riakgu.digilo.user;

import com.riakgu.digilo.auth.dto.AuthResponse;
import com.riakgu.digilo.category.dto.CategoryResponse;
import com.riakgu.digilo.common.exception.BadRequestException;
import com.riakgu.digilo.common.exception.DuplicateResourceException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.user.dto.ChangePasswordRequest;
import com.riakgu.digilo.user.dto.UpdateProfileRequest;
import com.riakgu.digilo.user.dto.UpdateRoleRequest;
import com.riakgu.digilo.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getEmail() != null) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPhone() != null) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new DuplicateResourceException("Phone number already exists");
            }
            user.setPhone(request.getPhone());
        }

        userRepository.save(user);

        log.info("User profile updated: userId={}", userId);

        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse changePassword(Long userId, ChangePasswordRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Old passwords don't match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("User password changed: userId={}", userId);

        return UserResponse.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponse::fromEntity);
    }

    @Transactional
    public UserResponse updateRole(Long userId, UpdateRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        user.setRole(request.getRole());
        userRepository.save(user);

        log.info("User role updated: userId={}, newRole={}", userId, request.getRole());

        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse updateStatus(Long userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        user.setStatus(status);
        userRepository.save(user);

        log.info("User status updated: userId={}, newStatus={}", userId, status);

        return UserResponse.fromEntity(user);
    }

}

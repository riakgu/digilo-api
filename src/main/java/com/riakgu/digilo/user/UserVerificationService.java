package com.riakgu.digilo.user;

import com.riakgu.digilo.common.exception.BadRequestException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.common.service.OtpService;
import com.riakgu.digilo.notification.NotificationSenderService;
import com.riakgu.digilo.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserVerificationService {

    private final OtpService otpService;
    private final NotificationSenderService notificationSender;
    private final UserRepository userRepository;

    public void sendEmailOtp(Long userId) {
        User user = findUser(userId);

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email already verified");
        }

        String otp = otpService.generateAndSaveOtp("email:" + user.getEmail());
        notificationSender.sendEmailOtp(user.getName(), user.getEmail(), otp);

        log.info("Email OTP sent to userId={}", userId);
    }

    @Transactional
    public UserResponse verifyEmail(Long userId, String otp) {
        User user = findUser(userId);

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email already verified");
        }

        otpService.validateOtp("email:" + user.getEmail(), otp);

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(Instant.now());
        userRepository.save(user);

        log.info("Email verified for userId={}", userId);
        return UserResponse.fromEntity(user);
    }

    public void sendPhoneOtp(Long userId) {
        User user = findUser(userId);

        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new BadRequestException("Phone number not set");
        }

        if (Boolean.TRUE.equals(user.getPhoneVerified())) {
            throw new BadRequestException("Phone already verified");
        }

        String otp = otpService.generateAndSaveOtp("phone:" + user.getPhone());
        notificationSender.sendWhatsAppOtp(user.getName(), user.getPhone(), otp);

        log.info("Phone OTP sent to userId={}", userId);
    }

    @Transactional
    public UserResponse verifyPhone(Long userId, String otp) {
        User user = findUser(userId);

        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new BadRequestException("Phone number not set");
        }

        if (Boolean.TRUE.equals(user.getPhoneVerified())) {
            throw new BadRequestException("Phone already verified");
        }

        otpService.validateOtp("phone:" + user.getPhone(), otp);

        user.setPhoneVerified(true);
        user.setPhoneVerifiedAt(Instant.now());
        userRepository.save(user);

        log.info("Phone verified for userId={}", userId);
        return UserResponse.fromEntity(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}

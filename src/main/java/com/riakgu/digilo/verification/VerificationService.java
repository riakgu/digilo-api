package com.riakgu.digilo.verification;

import com.riakgu.digilo.common.exception.BadRequestException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.messaging.EmailService;
import com.riakgu.digilo.messaging.WhatsAppService;
import com.riakgu.digilo.user.User;
import com.riakgu.digilo.user.UserRepository;
import com.riakgu.digilo.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final OtpService otpService;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final UserRepository userRepository;

    public void sendEmailOtp(Long userId) {
        User user = findUser(userId);

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email already verified");
        }

        String otp = otpService.generateAndSaveOtp("email:" + user.getEmail());
        emailService.sendVerificationEmail(user.getEmail(), otp);
        
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
        whatsAppService.sendOtp(user.getPhone(), otp);
        
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

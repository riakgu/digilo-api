package com.riakgu.digilo.verification;

import com.riakgu.digilo.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final StringRedisTemplate redisTemplate;

    private static final String OTP_PREFIX = "otp:verify:";
    private static final String COOLDOWN_PREFIX = "otp:cooldown:";
    private static final int OTP_LENGTH = 6;
    private static final Duration OTP_EXPIRY = Duration.ofMinutes(5);
    private static final Duration COOLDOWN = Duration.ofSeconds(60);

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateAndSaveOtp(String key) {
        // Check cooldown
        String cooldownKey = COOLDOWN_PREFIX + key;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new BadRequestException("Please wait before requesting another OTP");
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", secureRandom.nextInt(1000000));

        // Save OTP with TTL
        String otpKey = OTP_PREFIX + key;
        redisTemplate.opsForValue().set(otpKey, otp, OTP_EXPIRY);

        // Set cooldown
        redisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN);

        log.info("OTP generated for key={}", key);
        return otp;
    }

    public boolean validateOtp(String key, String otp) {
        String otpKey = OTP_PREFIX + key;
        String storedOtp = redisTemplate.opsForValue().get(otpKey);

        if (storedOtp == null) {
            throw new BadRequestException("OTP expired or not found");
        }

        if (!storedOtp.equals(otp)) {
            throw new BadRequestException("Invalid OTP");
        }

        // Delete OTP after successful validation
        redisTemplate.delete(otpKey);
        log.info("OTP validated for key={}", key);
        return true;
    }

    public void deleteOtp(String key) {
        redisTemplate.delete(OTP_PREFIX + key);
        redisTemplate.delete(COOLDOWN_PREFIX + key);
    }
}

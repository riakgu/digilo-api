package com.riakgu.digilo.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.riakgu.digilo.auth.dto.SessionResponse;
import com.riakgu.digilo.common.exception.UnauthorizedException;
import com.riakgu.digilo.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // --- Token Generation ---

    public String generateAccessToken(Long userId, String role, String sessionId) {
        return JWT.create()
                .withIssuer(properties.issuer())
                .withSubject(userId.toString())
                .withClaim("role", role)
                .withClaim("type", "access")
                .withClaim("sid", sessionId)
                .withExpiresAt(Instant.now().plusSeconds(properties.accessExpiration()))
                .sign(Algorithm.HMAC256(properties.accessSecret()));
    }

    public String generateRefreshToken(Long userId, String role, String sessionId, String deviceInfo) {
        String token = JWT.create()
                .withIssuer(properties.issuer())
                .withSubject(userId.toString())
                .withClaim("role", role)
                .withClaim("type", "refresh")
                .withClaim("sid", sessionId)
                .withJWTId(UUID.randomUUID().toString())
                .withExpiresAt(Instant.now().plusSeconds(properties.refreshExpiration()))
                .sign(Algorithm.HMAC256(properties.refreshSecret()));

        // Store session data as JSON in Redis
        String sessionKey = "refresh:" + userId + ":" + sessionId;
        try {
            Map<String, Object> sessionData = new LinkedHashMap<>();
            sessionData.put("token", token);
            sessionData.put("deviceInfo", deviceInfo != null ? deviceInfo : "Unknown");
            sessionData.put("createdAt", Instant.now().toString());

            String json = objectMapper.writeValueAsString(sessionData);
            redisTemplate.opsForValue().set(sessionKey, json,
                    Duration.ofSeconds(properties.refreshExpiration()));
        } catch (Exception e) {
            log.error("Failed to store session data: {}", e.getMessage());
            // Fallback: store just the token
            redisTemplate.opsForValue().set(sessionKey, token,
                    Duration.ofSeconds(properties.refreshExpiration()));
        }

        return token;
    }

    // --- Token Verification ---

    public DecodedJWT verifyAccessToken(String token) {
        return verify(token, properties.accessSecret(), "access");
    }

    public DecodedJWT verifyRefreshToken(String token) {
        DecodedJWT decoded = verify(token, properties.refreshSecret(), "refresh");

        String userId = decoded.getSubject();
        String sessionId = decoded.getClaim("sid").asString();

        if (sessionId == null) {
            throw new UnauthorizedException("Invalid refresh token: missing session");
        }

        String sessionKey = "refresh:" + userId + ":" + sessionId;
        String stored = redisTemplate.opsForValue().get(sessionKey);

        if (stored == null) {
            throw new UnauthorizedException("Session expired or revoked");
        }

        // Extract token from JSON or plain string
        String storedToken = extractTokenFromStored(stored);

        if (!token.equals(storedToken)) {
            // Reuse detection: this token was already rotated
            log.warn("Refresh token reuse detected: userId={}, sessionId={}", userId, sessionId);
            redisTemplate.delete(sessionKey);
            throw new UnauthorizedException("Refresh token reuse detected. Session revoked.");
        }

        return decoded;
    }

    private DecodedJWT verify(String token, String secret, String expectedType) {
        DecodedJWT decoded = JWT.require(Algorithm.HMAC256(secret))
                .build()
                .verify(token);

        if (!expectedType.equals(decoded.getClaim("type").asString())) {
            throw new UnauthorizedException("Invalid token type");
        }

        return decoded;
    }

    // --- Refresh Token Rotation ---

    public record RefreshResult(Long userId, String sessionId, String accessToken, String refreshToken) {
    }

    public RefreshResult refreshTokens(String refreshToken, String deviceInfo) {
        DecodedJWT decoded = verifyRefreshToken(refreshToken);

        Long userId = Long.valueOf(decoded.getSubject());
        String role = decoded.getClaim("role").asString();
        String sessionId = decoded.getClaim("sid").asString();

        String newAccessToken = generateAccessToken(userId, role, sessionId);
        String newRefreshToken = generateRefreshToken(userId, role, sessionId, deviceInfo);

        return new RefreshResult(userId, sessionId, newAccessToken, newRefreshToken);
    }

    // --- Token Blacklisting ---

    public void blacklistAccessToken(String token) {
        DecodedJWT decoded = JWT.decode(token);

        long ttl = decoded.getExpiresAt().toInstant().getEpochSecond()
                - Instant.now().getEpochSecond();

        if (ttl > 0) {
            redisTemplate.opsForValue()
                    .set("blacklist:" + token, "true", Duration.ofSeconds(ttl));
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
    }

    // --- Session Management ---

    public void revokeSession(Long userId, String sessionId) {
        String sessionKey = "refresh:" + userId + ":" + sessionId;
        redisTemplate.delete(sessionKey);
        log.info("Session revoked: userId={}, sessionId={}", userId, sessionId);
    }

    public void revokeUserTokens(Long userId) {
        Set<String> keys = redisTemplate.keys("refresh:" + userId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("All sessions revoked: userId={}, count={}", userId, keys.size());
        }
    }

    public List<SessionResponse> getActiveSessions(Long userId, String currentSessionId) {
        Set<String> keys = redisTemplate.keys("refresh:" + userId + ":*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<SessionResponse> sessions = new ArrayList<>();
        for (String key : keys) {
            String sessionId = key.substring(key.lastIndexOf(":") + 1);
            String stored = redisTemplate.opsForValue().get(key);
            if (stored == null)
                continue;

            SessionResponse session = parseSessionResponse(sessionId, stored, currentSessionId);
            sessions.add(session);
        }

        return sessions;
    }

    // --- Helpers ---

    private String extractTokenFromStored(String stored) {
        if (stored.startsWith("{")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(stored, Map.class);
                return (String) data.get("token");
            } catch (Exception e) {
                log.warn("Failed to parse session JSON, treating as raw token");
                return stored;
            }
        }
        return stored;
    }

    private SessionResponse parseSessionResponse(String sessionId, String stored, String currentSessionId) {
        String deviceInfo = "Unknown";
        Instant createdAt = null;

        if (stored.startsWith("{")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(stored, Map.class);
                deviceInfo = (String) data.getOrDefault("deviceInfo", "Unknown");
                String createdAtStr = (String) data.get("createdAt");
                if (createdAtStr != null) {
                    createdAt = Instant.parse(createdAtStr);
                }
            } catch (Exception e) {
                log.warn("Failed to parse session metadata for sessionId={}", sessionId);
            }
        }

        return SessionResponse.builder()
                .sessionId(sessionId)
                .deviceInfo(deviceInfo)
                .createdAt(createdAt)
                .current(sessionId.equals(currentSessionId))
                .build();
    }
}

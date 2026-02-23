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

    public String generateRefreshToken(Long userId, String role, String sessionId, String userAgent, String ip) {
        String token = JWT.create()
                .withIssuer(properties.issuer())
                .withSubject(userId.toString())
                .withClaim("role", role)
                .withClaim("type", "refresh")
                .withClaim("sid", sessionId)
                .withJWTId(UUID.randomUUID().toString())
                .withExpiresAt(Instant.now().plusSeconds(properties.refreshExpiration()))
                .sign(Algorithm.HMAC256(properties.refreshSecret()));

        storeSession(userId, sessionId, token, userAgent, ip);
        return token;
    }

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

        SessionData session = getSessionData(sessionKey(userId, sessionId));

        if (session == null) {
            throw new UnauthorizedException("Session expired or revoked");
        }

        if (!token.equals(session.token())) {
            log.warn("Refresh token reuse detected: userId={}, sessionId={}", userId, sessionId);
            redisTemplate.delete(sessionKey(userId, sessionId));
            throw new UnauthorizedException("Refresh token reuse detected. Session revoked.");
        }

        return decoded;
    }

    public record RefreshResult(Long userId, String sessionId, String accessToken, String refreshToken) {
    }

    public RefreshResult refreshTokens(String refreshToken, String userAgent, String ip) {
        DecodedJWT decoded = verifyRefreshToken(refreshToken);

        Long userId = Long.valueOf(decoded.getSubject());
        String role = decoded.getClaim("role").asString();
        String sessionId = decoded.getClaim("sid").asString();

        String newAccessToken = generateAccessToken(userId, role, sessionId);
        String newRefreshToken = generateRefreshToken(userId, role, sessionId, userAgent, ip);

        return new RefreshResult(userId, sessionId, newAccessToken, newRefreshToken);
    }

    public void blacklistAccessToken(String token) {
        DecodedJWT decoded = JWT.decode(token);
        long ttl = decoded.getExpiresAt().toInstant().getEpochSecond() - Instant.now().getEpochSecond();

        if (ttl > 0) {
            redisTemplate.opsForValue().set("blacklist:" + token, "true", Duration.ofSeconds(ttl));
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
    }

    public void revokeSession(Long userId, String sessionId) {
        redisTemplate.delete(sessionKey(userId.toString(), sessionId));
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
            SessionData data = getSessionData(key);
            if (data == null) continue;

            sessions.add(SessionResponse.builder()
                    .sessionId(sessionId)
                    .userAgent(data.userAgent())
                    .ip(data.ip())
                    .createdAt(data.createdAt())
                    .current(sessionId.equals(currentSessionId))
                    .build());
        }

        return sessions;
    }

    private record SessionData(String token, String userAgent, String ip, Instant createdAt) {
    }

    private String sessionKey(Object userId, String sessionId) {
        return "refresh:" + userId + ":" + sessionId;
    }

    private void storeSession(Long userId, String sessionId, String token, String userAgent, String ip) {
        String key = sessionKey(userId, sessionId);
        try {
            Map<String, Object> sessionData = new LinkedHashMap<>();
            sessionData.put("token", token);
            sessionData.put("userAgent", userAgent != null ? userAgent : "Unknown");
            sessionData.put("ip", ip != null ? ip : "Unknown");
            sessionData.put("createdAt", Instant.now().toString());

            String json = objectMapper.writeValueAsString(sessionData);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(properties.refreshExpiration()));
        } catch (Exception e) {
            log.error("Failed to store session data: {}", e.getMessage());
            redisTemplate.opsForValue().set(key, token, Duration.ofSeconds(properties.refreshExpiration()));
        }
    }

    private SessionData getSessionData(String key) {
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) return null;

        if (stored.startsWith("{")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(stored, Map.class);
                String token = (String) data.get("token");
                String userAgent = (String) data.getOrDefault("userAgent", "Unknown");
                String ip = (String) data.getOrDefault("ip", "Unknown");
                String createdAtStr = (String) data.get("createdAt");
                Instant createdAt = createdAtStr != null ? Instant.parse(createdAtStr) : null;
                return new SessionData(token, userAgent, ip, createdAt);
            } catch (Exception e) {
                log.warn("Failed to parse session JSON, treating as raw token");
                return new SessionData(stored, "Unknown", "Unknown", null);
            }
        }

        return new SessionData(stored, "Unknown", "Unknown", null);
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
}

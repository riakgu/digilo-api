package com.riakgu.digilo.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.riakgu.digilo.common.exception.UnauthorizedException;
import com.riakgu.digilo.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;
    private final StringRedisTemplate redisTemplate;

    public String generateAccessToken(Long userId, String role) {
        return JWT.create()
                .withIssuer(properties.getIssuer())
                .withSubject(userId.toString())
                .withClaim("role", role)
                .withClaim("type", "access")
                .withExpiresAt(Instant.now().plusSeconds(properties.getAccessExpiration()))
                .sign(Algorithm.HMAC256(properties.getAccessSecret()));
    }

    public String generateRefreshToken(Long userId, String role) {
        String token = JWT.create()
                .withIssuer(properties.getIssuer())
                .withSubject(userId.toString())
                .withClaim("role", role)
                .withClaim("type", "refresh")
                .withExpiresAt(Instant.now().plusSeconds(properties.getRefreshExpiration()))
                .sign(Algorithm.HMAC256(properties.getRefreshSecret()));

        redisTemplate.opsForValue().set(
                "refresh:" + userId,
                token,
                Duration.ofSeconds(properties.getRefreshExpiration())
        );

        return token;
    }

    public DecodedJWT verifyAccessToken(String token) {
        return verify(token, properties.getAccessSecret(), "access");
    }

    public DecodedJWT verifyRefreshToken(String token) {
        DecodedJWT decoded = verify(token, properties.getRefreshSecret(), "refresh");

        String userId = decoded.getSubject();
        String stored = redisTemplate.opsForValue().get("refresh:" + userId);

        if (!token.equals(stored)) {
            throw new UnauthorizedException("Invalid refresh token");
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

    public String refreshAccessToken(String refreshToken) {
        DecodedJWT decoded = verifyRefreshToken(refreshToken);

        Long userId = Long.valueOf(decoded.getSubject());
        String role =  decoded.getClaim("role").asString();

        return generateAccessToken(userId, role);
    }

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

    public void revokeUserTokens(Long userId) {
        redisTemplate.delete("refresh:" + userId);
    }

}

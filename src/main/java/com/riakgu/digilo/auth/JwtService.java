package com.riakgu.digilo.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;
    private final StringRedisTemplate redisTemplate;

    public String generateAccessToken(Long userId, String role) {
        return JWT.create()
                .withIssuer(props.getIssuer())
                .withSubject(userId.toString())
                .withClaim("role", role)
                .withClaim("type", "access")
                .withExpiresAt(Instant.now().plusSeconds(props.getAccessExpiration()))
                .sign(Algorithm.HMAC256(props.getAccessSecret()));
    }

    public String generateRefreshToken(Long userId) {
        String token = JWT.create()
                .withIssuer(props.getIssuer())
                .withSubject(userId.toString())
                .withClaim("type", "refresh")
                .withExpiresAt(Instant.now().plusSeconds(props.getRefreshExpiration()))
                .sign(Algorithm.HMAC256(props.getRefreshSecret()));

        redisTemplate.opsForValue().set(
                "refresh:" + userId,
                token,
                Duration.ofSeconds(props.getRefreshExpiration())
        );

        return token;
    }

    public DecodedJWT verifyAccessToken(String token) {
        return verify(token, props.getAccessSecret(), "access");
    }

    public DecodedJWT verifyRefreshToken(String token) {
        DecodedJWT decoded = verify(token, props.getRefreshSecret(), "refresh");

        String userId = decoded.getSubject();
        String stored = redisTemplate.opsForValue().get("refresh:" + userId);

        if (!token.equals(stored)) {
            throw new RuntimeException("Invalid refresh token");
        }

        return decoded;
    }

    private DecodedJWT verify(String token, String secret, String expectedType) {
        DecodedJWT decoded = JWT.require(Algorithm.HMAC256(secret))
                .build()
                .verify(token);

        if (!expectedType.equals(decoded.getClaim("type").asString())) {
            throw new RuntimeException("Invalid token type");
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

package com.riakgu.digilo.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
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

    public String generateAccessToken(Long userId) {
        return JWT.create()
                .withIssuer(props.getIssuer())
                .withSubject(userId.toString())
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

}

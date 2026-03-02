package com.riakgu.digilo.security;

import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RateLimitProperties rateLimitProperties;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";

    /**
     * Atomic Lua script for rate limiting.
     * KEYS[1] = rate limit key
     * ARGV[1] = limit (max requests per window)
     * ARGV[2] = window size in seconds
     *
     * Returns: "current_count ttl"
     * - current_count: the request count after incrementing
     * - ttl: seconds until the window resets
     */
    private static final RedisScript<String> RATE_LIMIT_SCRIPT = RedisScript.of("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
            end
            local ttl = redis.call('TTL', KEYS[1])
            if ttl < 0 then
                ttl = tonumber(ARGV[2])
            end
            return current .. ' ' .. ttl
            """, String.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        
        // Skip non-API paths (actuator, swagger, etc.)
        if (!path.startsWith("/api")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Determine rate limit based on context
        int limit = determineLimit(path);
        String rateLimitKey = buildRateLimitKey(request, path);

        // Check rate limit atomically via Lua script
        RateLimitResult result;
        try {
            result = checkRateLimit(rateLimitKey, limit);
        } catch (Exception e) {
            // Fail-open: allow request if Redis is unavailable
            log.warn("Rate limiting unavailable (Redis connection issue): {}. Allowing request.", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // Always add rate limit headers
        addRateLimitHeaders(response, limit, result.remaining(), result.resetSeconds());

        if (result.exceeded()) {
            sendRateLimitExceededResponse(response, path, result.resetSeconds());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int determineLimit(String path) {
        // Stricter limits for authentication endpoints
        if (isAuthEndpoint(path)) {
            return rateLimitProperties.authEndpointLimit();
        }

        // Check if user is authenticated
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Long) {
            return rateLimitProperties.authenticatedLimit();
        }

        return rateLimitProperties.defaultLimit();
    }

    private boolean isAuthEndpoint(String path) {
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/password");
    }

    private String buildRateLimitKey(HttpServletRequest request, String path) {
        String identifier;

        // Use user ID for authenticated users, IP for anonymous
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Long userId) {
            identifier = "user:" + userId;
        } else {
            identifier = "ip:" + getClientIp(request);
        }

        // Separate bucket for auth endpoints
        if (isAuthEndpoint(path)) {
            return RATE_LIMIT_PREFIX + "auth:" + identifier;
        }

        return RATE_LIMIT_PREFIX + identifier;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private RateLimitResult checkRateLimit(String key, int limit) {
        int windowSeconds = rateLimitProperties.windowSeconds();

        String scriptResult = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(key),
                String.valueOf(limit),
                String.valueOf(windowSeconds)
        );

        String[] parts = scriptResult.split(" ");
        long currentCount = Long.parseLong(parts[0]);
        int ttl = Integer.parseInt(parts[1]);

        int remaining = Math.max(0, limit - (int) currentCount);
        boolean exceeded = currentCount > limit;

        return new RateLimitResult(exceeded, remaining, Math.max(1, ttl));
    }

    private void addRateLimitHeaders(HttpServletResponse response, int limit, int remaining, int resetSeconds) {
        response.setHeader(HEADER_LIMIT, String.valueOf(limit));
        response.setHeader(HEADER_REMAINING, String.valueOf(remaining));
        response.setHeader(HEADER_RESET, String.valueOf(resetSeconds));
    }

    private void sendRateLimitExceededResponse(HttpServletResponse response, String path, int resetSeconds) 
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");

        String message = String.format("Rate limit exceeded. Please try again in %d seconds.", resetSeconds);
        ApiResponse<Object> apiResponse = ApiResponse.error("TOO_MANY_REQUESTS", message, path);

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        
        log.warn("Rate limit exceeded for path: {}", path);
    }

    private record RateLimitResult(boolean exceeded, int remaining, int resetSeconds) {}
}
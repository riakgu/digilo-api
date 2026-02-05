package com.riakgu.digilo;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.riakgu.digilo.user.Role;
import com.riakgu.digilo.user.User;
import com.riakgu.digilo.user.UserRepository;
import com.riakgu.digilo.user.UserStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

/**
 * Test utility class providing common test operations.
 */
public class TestHelper {

    private static final String ACCESS_SECRET = "test-access-secret-for-testing-only";
    private static final String ISSUER = "digilo-test";
    private static final long ACCESS_EXPIRATION = 3600L;
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    public static final String DEFAULT_PASSWORD = "password123";

    private TestHelper() {
    }

    /**
     * Creates and saves a test user with USER role.
     */
    public static User createTestUser(UserRepository userRepository) {
        return createTestUser(userRepository, "testuser@example.com", "Test User", Role.USER);
    }

    /**
     * Creates and saves a test user with specified email and name.
     */
    public static User createTestUser(UserRepository userRepository, String email, String name, Role role) {
        User user = User.builder()
                .email(email)
                .name(name)
                .password(PASSWORD_ENCODER.encode(DEFAULT_PASSWORD))
                .role(role)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(false)
                .build();
        return userRepository.save(user);
    }

    /**
     * Creates and saves an admin user.
     */
    public static User createAdminUser(UserRepository userRepository) {
        return createTestUser(userRepository, "admin@example.com", "Admin User", Role.ADMIN);
    }

    /**
     * Generates a valid JWT access token for testing.
     */
    public static String generateAccessToken(Long userId, Role role) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(userId.toString())
                .withClaim("role", role.name())
                .withClaim("type", "access")
                .withExpiresAt(Instant.now().plusSeconds(ACCESS_EXPIRATION))
                .sign(Algorithm.HMAC256(ACCESS_SECRET));
    }

    /**
     * Returns the Authorization header value (Bearer token).
     */
    public static String getAuthHeader(Long userId, Role role) {
        return "Bearer " + generateAccessToken(userId, role);
    }

    /**
     * Returns the Authorization header with default USER role.
     */
    public static String getAuthHeader(Long userId) {
        return getAuthHeader(userId, Role.USER);
    }

    /**
     * Returns raw password encoder for password verification in tests.
     */
    public static PasswordEncoder getPasswordEncoder() {
        return PASSWORD_ENCODER;
    }
}

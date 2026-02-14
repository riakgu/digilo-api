package com.riakgu.digilo.auth;

import com.riakgu.digilo.TestHelper;
import com.riakgu.digilo.auth.dto.AuthResponse;
import com.riakgu.digilo.auth.dto.LoginRequest;
import com.riakgu.digilo.auth.dto.RefreshRequest;
import com.riakgu.digilo.auth.dto.RegisterRequest;
import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.config.TestMockConfig;
import com.riakgu.digilo.config.TestContainersConfig;
import com.riakgu.digilo.user.Role;
import com.riakgu.digilo.user.User;
import com.riakgu.digilo.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@ActiveProfiles("test")
@Import({ TestContainersConfig.class, TestMockConfig.class })
@Transactional
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private JwtService jwtService;

        @Autowired
        private StringRedisTemplate redisTemplate;

        @BeforeEach
        void setUp() {
                userRepository.deleteAll();
                redisTemplate.delete(redisTemplate.keys("refresh:*"));
                redisTemplate.delete(redisTemplate.keys("blacklist:*"));
        }

        // ==================== REGISTER ====================

        @Test
        void registerSuccess() throws Exception {
                RegisterRequest request = new RegisterRequest();
                request.setEmail("newuser@example.com");
                request.setPassword("password123");
                request.setName("New User");

                mockMvc.perform(
                                post("/api/auth/register")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpectAll(
                                                status().isCreated())
                                .andDo(result -> {
                                        ApiResponse<AuthResponse> response = objectMapper.readValue(
                                                        result.getResponse().getContentAsString(),
                                                        new TypeReference<>() {
                                                        });

                                        assertNull(response.getErrors());
                                        assertNotNull(response.getData().getAccessToken());
                                        assertNotNull(response.getData().getRefreshToken());
                                        assertNotNull(response.getData().getUser());
                                        assertEquals("newuser@example.com", response.getData().getUser().getEmail());
                                        assertEquals("New User", response.getData().getUser().getName());

                                        assertTrue(userRepository.findByEmail("newuser@example.com").isPresent());
                                });
        }

        @Test
        void registerBadRequestEmptyFields() throws Exception {
                RegisterRequest request = new RegisterRequest();
                request.setEmail("");
                request.setPassword("");
                request.setName("");

                mockMvc.perform(
                                post("/api/auth/register")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpectAll(
                                                status().isBadRequest())
                                .andDo(result -> {
                                        ApiResponse<String> response = objectMapper.readValue(
                                                        result.getResponse().getContentAsString(),
                                                        new TypeReference<>() {
                                                        });

                                        assertNotNull(response.getErrors());
                                });
        }

        @Test
        void registerDuplicateEmail() throws Exception {
                TestHelper.createTestUser(userRepository, "existing@example.com", "Existing User", Role.USER);

                RegisterRequest request = new RegisterRequest();
                request.setEmail("existing@example.com");
                request.setPassword("password123");
                request.setName("Duplicate User");

                mockMvc.perform(
                                post("/api/auth/register")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpectAll(
                                                status().isConflict())
                                .andDo(result -> {
                                        ApiResponse<String> response = objectMapper.readValue(
                                                        result.getResponse().getContentAsString(),
                                                        new TypeReference<>() {
                                                        });

                                        assertNotNull(response.getMessage());
                                });
        }

        // ==================== LOGIN ====================

        @Test
        void loginSuccess() throws Exception {
                User user = TestHelper.createTestUser(userRepository);

                LoginRequest request = new LoginRequest();
                request.setEmail(user.getEmail());
                request.setPassword(TestHelper.DEFAULT_PASSWORD);

                mockMvc.perform(
                                post("/api/auth/login")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpectAll(
                                                status().isOk())
                                .andDo(result -> {
                                        ApiResponse<AuthResponse> response = objectMapper.readValue(
                                                        result.getResponse().getContentAsString(),
                                                        new TypeReference<>() {
                                                        });

                                        assertNull(response.getErrors());
                                        assertNotNull(response.getData().getAccessToken());
                                        assertNotNull(response.getData().getRefreshToken());
                                        assertNotNull(response.getData().getUser());
                                        assertEquals(user.getEmail(), response.getData().getUser().getEmail());
                                });
        }

        @Test
        void loginFailedUserNotFound() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setEmail("nonexistent@example.com");
                request.setPassword("password123");

                mockMvc.perform(
                                post("/api/auth/login")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpectAll(
                                                status().isUnauthorized())
                                .andDo(result -> {
                                        ApiResponse<String> response = objectMapper.readValue(
                                                        result.getResponse().getContentAsString(),
                                                        new TypeReference<>() {
                                                        });

                                        assertNotNull(response.getMessage());
                                });
        }

        @Test
        void loginFailedWrongPassword() throws Exception {
                User user = TestHelper.createTestUser(userRepository);

                LoginRequest request = new LoginRequest();
                request.setEmail(user.getEmail());
                request.setPassword("wrongpassword");

                mockMvc.perform(
                                post("/api/auth/login")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpectAll(
                                                status().isUnauthorized())
                                .andDo(result -> {
                                        ApiResponse<String> response = objectMapper.readValue(
                                                        result.getResponse().getContentAsString(),
                                                        new TypeReference<>() {
                                                        });

                                        assertNotNull(response.getMessage());
                                });
        }

        // ==================== REFRESH ====================

        @Test
        void refreshSuccess() throws Exception {
                User user = TestHelper.createTestUser(userRepository);
                String sessionId = java.util.UUID.randomUUID().toString();

                String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getRole().name(), sessionId,
                                "Test Device");

                RefreshRequest request = new RefreshRequest();
                request.setRefreshToken(refreshToken);

                mockMvc.perform(
                                post("/api/auth/refresh")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpectAll(
                                                status().isOk())
                                .andDo(result -> {
                                        ApiResponse<AuthResponse> response = objectMapper.readValue(
                                                        result.getResponse().getContentAsString(),
                                                        new TypeReference<>() {
                                                        });

                                        assertNull(response.getErrors());
                                        assertNotNull(response.getData().getAccessToken());
                                        assertNotNull(response.getData().getRefreshToken());
                                        assertNotNull(response.getData().getUser());
                                        assertEquals(user.getEmail(), response.getData().getUser().getEmail());
                                });
        }

        @Test
        void refreshFailedInvalidToken() throws Exception {
                RefreshRequest request = new RefreshRequest();
                request.setRefreshToken("invalid.token.here");

                mockMvc.perform(
                                post("/api/auth/refresh")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(result -> {
                                        int status = result.getResponse().getStatus();
                                        assertTrue(status == 401 || status == 500,
                                                        "Expected 401 or 500, got " + status);
                                });
        }

        @Test
        void refreshTokenReuseDetection() throws Exception {
                User user = TestHelper.createTestUser(userRepository);
                String sessionId = java.util.UUID.randomUUID().toString();

                String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getRole().name(), sessionId,
                                "Test Device");

                // First refresh should succeed and rotate the token
                RefreshRequest request = new RefreshRequest();
                request.setRefreshToken(refreshToken);

                mockMvc.perform(
                                post("/api/auth/refresh")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                // Reusing the old refresh token should fail (reuse detection)
                mockMvc.perform(
                                post("/api/auth/refresh")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(result -> {
                                        int status = result.getResponse().getStatus();
                                        assertTrue(status == 401 || status == 500,
                                                        "Expected 401 or 500, got " + status);
                                });
        }

        // ==================== LOGOUT ====================

        @Test
        void logoutSuccess() throws Exception {
                User user = TestHelper.createTestUser(userRepository);
                String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

                mockMvc.perform(
                                post("/api/auth/logout")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .header("Authorization", authHeader))
                                .andExpectAll(
                                                status().isOk())
                                .andDo(result -> {
                                        ApiResponse<Void> response = objectMapper.readValue(
                                                        result.getResponse().getContentAsString(),
                                                        new TypeReference<>() {
                                                        });

                                        assertNull(response.getErrors());
                                });
        }

        @Test
        void logoutUnauthorized() throws Exception {
                mockMvc.perform(
                                post("/api/auth/logout")
                                                .accept(MediaType.APPLICATION_JSON))
                                .andExpectAll(
                                                status().isUnauthorized());
        }

        @Test
        void logoutOnlyRevokesCurrentSession() throws Exception {
                User user = TestHelper.createTestUser(userRepository);

                // Create two sessions via login
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setEmail(user.getEmail());
                loginRequest.setPassword(TestHelper.DEFAULT_PASSWORD);

                // Session 1
                String[] session1Tokens = new String[2];
                mockMvc.perform(
                                post("/api/auth/login")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andDo(result -> {
                                        ApiResponse<AuthResponse> response = objectMapper.readValue(
                                                        result.getResponse().getContentAsString(),
                                                        new TypeReference<>() {
                                                        });
                                        session1Tokens[0] = response.getData().getAccessToken();
                                        session1Tokens[1] = response.getData().getRefreshToken();
                                });

                // Session 2
                String[] session2Tokens = new String[2];
                mockMvc.perform(
                                post("/api/auth/login")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andDo(result -> {
                                        ApiResponse<AuthResponse> response = objectMapper.readValue(
                                                        result.getResponse().getContentAsString(),
                                                        new TypeReference<>() {
                                                        });
                                        session2Tokens[0] = response.getData().getAccessToken();
                                        session2Tokens[1] = response.getData().getRefreshToken();
                                });

                // Logout session 1
                mockMvc.perform(
                                post("/api/auth/logout")
                                                .header("Authorization", "Bearer " + session1Tokens[0]))
                                .andExpect(status().isOk());

                // Session 2 refresh should still work
                RefreshRequest refreshRequest = new RefreshRequest();
                refreshRequest.setRefreshToken(session2Tokens[1]);

                mockMvc.perform(
                                post("/api/auth/refresh")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(refreshRequest)))
                                .andExpect(status().isOk());
        }

        @Test
        void logoutAllRevokesEverything() throws Exception {
                User user = TestHelper.createTestUser(userRepository);

                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setEmail(user.getEmail());
                loginRequest.setPassword(TestHelper.DEFAULT_PASSWORD);

                // Session 1
                String[] session1Tokens = new String[2];
                mockMvc.perform(
                                post("/api/auth/login")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andDo(result -> {
                                        ApiResponse<AuthResponse> response = objectMapper.readValue(
                                                        result.getResponse().getContentAsString(),
                                                        new TypeReference<>() {
                                                        });
                                        session1Tokens[0] = response.getData().getAccessToken();
                                        session1Tokens[1] = response.getData().getRefreshToken();
                                });

                // Session 2
                String[] session2Tokens = new String[2];
                mockMvc.perform(
                                post("/api/auth/login")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andDo(result -> {
                                        ApiResponse<AuthResponse> response = objectMapper.readValue(
                                                        result.getResponse().getContentAsString(),
                                                        new TypeReference<>() {
                                                        });
                                        session2Tokens[0] = response.getData().getAccessToken();
                                        session2Tokens[1] = response.getData().getRefreshToken();
                                });

                // Logout all from session 1
                mockMvc.perform(
                                post("/api/auth/logout-all")
                                                .header("Authorization", "Bearer " + session1Tokens[0]))
                                .andExpect(status().isOk());

                // Session 2 refresh should fail
                RefreshRequest refreshRequest = new RefreshRequest();
                refreshRequest.setRefreshToken(session2Tokens[1]);

                mockMvc.perform(
                                post("/api/auth/refresh")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(refreshRequest)))
                                .andDo(result -> {
                                        int status = result.getResponse().getStatus();
                                        assertTrue(status == 401 || status == 500,
                                                        "Expected 401 or 500, got " + status);
                                });
        }

        // ==================== SESSION MANAGEMENT ====================

        @Test
        void listActiveSessions() throws Exception {
                User user = TestHelper.createTestUser(userRepository);

                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setEmail(user.getEmail());
                loginRequest.setPassword(TestHelper.DEFAULT_PASSWORD);

                // Create 2 sessions
                String[] accessToken1 = new String[1];
                mockMvc.perform(
                                post("/api/auth/login")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andDo(result -> {
                                        ApiResponse<AuthResponse> response = objectMapper.readValue(
                                                        result.getResponse().getContentAsString(),
                                                        new TypeReference<>() {
                                                        });
                                        accessToken1[0] = response.getData().getAccessToken();
                                });

                mockMvc.perform(
                                post("/api/auth/login")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk());

                // List sessions
                mockMvc.perform(
                                get("/api/auth/sessions")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .header("Authorization", "Bearer " + accessToken1[0]))
                                .andExpectAll(
                                                status().isOk())
                                .andDo(result -> {
                                        String json = result.getResponse().getContentAsString();
                                        assertTrue(json.contains("sessionId"));
                                        // Should have at least 2 sessions
                                        int count = json.split("sessionId").length - 1;
                                        assertTrue(count >= 2, "Expected at least 2 sessions, found " + count);
                                });
        }

        @Test
        void revokeSpecificSession() throws Exception {
                User user = TestHelper.createTestUser(userRepository);

                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setEmail(user.getEmail());
                loginRequest.setPassword(TestHelper.DEFAULT_PASSWORD);

                // Session 1 (the one we'll use to revoke)
                String[] session1Token = new String[1];
                mockMvc.perform(
                                post("/api/auth/login")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andDo(result -> {
                                        ApiResponse<AuthResponse> response = objectMapper.readValue(
                                                        result.getResponse().getContentAsString(),
                                                        new TypeReference<>() {
                                                        });
                                        session1Token[0] = response.getData().getAccessToken();
                                });

                // Session 2 (the one we'll revoke)
                String[] session2Tokens = new String[2];
                mockMvc.perform(
                                post("/api/auth/login")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andDo(result -> {
                                        ApiResponse<AuthResponse> response = objectMapper.readValue(
                                                        result.getResponse().getContentAsString(),
                                                        new TypeReference<>() {
                                                        });
                                        session2Tokens[0] = response.getData().getAccessToken();
                                        session2Tokens[1] = response.getData().getRefreshToken();
                                });

                // Extract session2's sessionId from its access token
                com.auth0.jwt.interfaces.DecodedJWT decoded = com.auth0.jwt.JWT.decode(session2Tokens[0]);
                String session2Id = decoded.getClaim("sid").asString();

                // Revoke session 2 from session 1
                mockMvc.perform(
                                delete("/api/auth/sessions/" + session2Id)
                                                .accept(MediaType.APPLICATION_JSON)
                                                .header("Authorization", "Bearer " + session1Token[0]))
                                .andExpect(status().isOk());

                // Session 2 refresh should fail
                RefreshRequest refreshRequest = new RefreshRequest();
                refreshRequest.setRefreshToken(session2Tokens[1]);

                mockMvc.perform(
                                post("/api/auth/refresh")
                                                .accept(MediaType.APPLICATION_JSON)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(refreshRequest)))
                                .andDo(result -> {
                                        int status = result.getResponse().getStatus();
                                        assertTrue(status == 401 || status == 500,
                                                        "Expected 401 or 500, got " + status);
                                });
        }
}

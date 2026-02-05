package com.riakgu.digilo.user;

import com.riakgu.digilo.TestHelper;
import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.config.TestMockConfig;
import com.riakgu.digilo.user.dto.AdminUpdateUserRequest;
import com.riakgu.digilo.user.dto.ChangePasswordRequest;
import com.riakgu.digilo.user.dto.UpdateProfileRequest;
import com.riakgu.digilo.user.dto.UserResponse;
import com.riakgu.digilo.user.dto.VerifyOtpRequest;
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
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@ActiveProfiles("test")
@Import(TestMockConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String OTP_PREFIX = "otp:verify:";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        // Clear OTP keys
        var keys = redisTemplate.keys(OTP_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        // Clear cooldown keys
        var cooldownKeys = redisTemplate.keys("otp:cooldown:*");
        if (cooldownKeys != null && !cooldownKeys.isEmpty()) {
            redisTemplate.delete(cooldownKeys);
        }
    }

    // ==================== GET PROFILE ====================

    @Test
    void getProfileSuccess() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        mockMvc.perform(
                get("/api/user/profile")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<UserResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertEquals(user.getEmail(), response.getData().getEmail());
            assertEquals(user.getName(), response.getData().getName());
        });
    }

    @Test
    void getProfileUnauthorized() throws Exception {
        mockMvc.perform(
                get("/api/user/profile")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isUnauthorized()
        );
    }

    // ==================== UPDATE PROFILE ====================

    @Test
    void updateProfileSuccess() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Updated Name");

        mockMvc.perform(
                patch("/api/user/profile")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<UserResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals("Updated Name", response.getData().getName());

            // Verify in database
            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertEquals("Updated Name", updated.getName());
        });
    }

    @Test
    void updateProfileBadRequestInvalidEmail() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("invalid-email");

        mockMvc.perform(
                patch("/api/user/profile")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        );
    }

    @Test
    void updateProfileBadRequestInvalidPhone() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("123"); // Invalid phone format

        mockMvc.perform(
                patch("/api/user/profile")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        );
    }

    @Test
    void updateProfileDuplicateEmail() throws Exception {
        // Create two users
        User user1 = TestHelper.createTestUser(userRepository);
        User user2 = TestHelper.createTestUser(userRepository, "other@example.com", "Other User", Role.USER);
        String authHeader = TestHelper.getAuthHeader(user1.getId(), user1.getRole());

        // Try to update user1's email to user2's email
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail(user2.getEmail());

        mockMvc.perform(
                patch("/api/user/profile")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isConflict()
        );
    }

    // ==================== CHANGE PASSWORD ====================

    @Test
    void changePasswordSuccess() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword(TestHelper.DEFAULT_PASSWORD);
        request.setNewPassword("newpassword123");

        mockMvc.perform(
                patch("/api/user/password")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<UserResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());

            // Verify password was changed
            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertTrue(TestHelper.getPasswordEncoder().matches("newpassword123", updated.getPassword()));
        });
    }

    @Test
    void changePasswordBadRequestWrongOldPassword() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrongpassword");
        request.setNewPassword("newpassword123");

        mockMvc.perform(
                patch("/api/user/password")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        );
    }

    @Test
    void changePasswordBadRequestShortPassword() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword(TestHelper.DEFAULT_PASSWORD);
        request.setNewPassword("short"); // Less than 8 characters

        mockMvc.perform(
                patch("/api/user/password")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        );
    }

    // ==================== EMAIL VERIFICATION ====================

    @Test
    void sendEmailOtpSuccess() throws Exception {
        // Create user with unverified email
        User user = User.builder()
                .email("unverified@example.com")
                .name("Unverified User")
                .password(TestHelper.getPasswordEncoder().encode(TestHelper.DEFAULT_PASSWORD))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(false)
                .build();
        user = userRepository.save(user);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        mockMvc.perform(
                post("/api/user/verify/email/send")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );
    }

    @Test
    void sendEmailOtpBadRequestAlreadyVerified() throws Exception {
        User user = TestHelper.createTestUser(userRepository); // Already verified
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        mockMvc.perform(
                post("/api/user/verify/email/send")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isBadRequest()
        );
    }

    @Test
    void verifyEmailSuccess() throws Exception {
        // Create user with unverified email
        User user = User.builder()
                .email("unverified@example.com")
                .name("Unverified User")
                .password(TestHelper.getPasswordEncoder().encode(TestHelper.DEFAULT_PASSWORD))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(false)
                .build();
        user = userRepository.save(user);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        // Set OTP in Redis
        String otp = "123456";
        redisTemplate.opsForValue().set(OTP_PREFIX + "email:" + user.getEmail(), otp, Duration.ofMinutes(5));

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setOtp(otp);

        User finalUser = user;
        mockMvc.perform(
                post("/api/user/verify/email")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<UserResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertTrue(response.getData().getEmailVerified());

            // Verify in database
            User updated = userRepository.findById(finalUser.getId()).orElseThrow();
            assertTrue(updated.getEmailVerified());
        });
    }

    @Test
    void verifyEmailBadRequestWrongOtp() throws Exception {
        User user = User.builder()
                .email("unverified@example.com")
                .name("Unverified User")
                .password(TestHelper.getPasswordEncoder().encode(TestHelper.DEFAULT_PASSWORD))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(false)
                .build();
        user = userRepository.save(user);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        // Set OTP in Redis
        redisTemplate.opsForValue().set(OTP_PREFIX + "email:" + user.getEmail(), "123456", Duration.ofMinutes(5));

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setOtp("000000"); // Wrong OTP

        mockMvc.perform(
                post("/api/user/verify/email")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        );
    }

    // ==================== PHONE VERIFICATION ====================

    @Test
    void sendPhoneOtpSuccess() throws Exception {
        User user = User.builder()
                .email("phonetest@example.com")
                .name("Phone User")
                .phone("+6281234567890")
                .password(TestHelper.getPasswordEncoder().encode(TestHelper.DEFAULT_PASSWORD))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(false)
                .build();
        user = userRepository.save(user);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        mockMvc.perform(
                post("/api/user/verify/phone/send")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );
    }

    @Test
    void sendPhoneOtpBadRequestNoPhone() throws Exception {
        User user = TestHelper.createTestUser(userRepository); // No phone set
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        mockMvc.perform(
                post("/api/user/verify/phone/send")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isBadRequest()
        );
    }

    @Test
    void verifyPhoneSuccess() throws Exception {
        User user = User.builder()
                .email("phonetest@example.com")
                .name("Phone User")
                .phone("+6281234567890")
                .password(TestHelper.getPasswordEncoder().encode(TestHelper.DEFAULT_PASSWORD))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(false)
                .build();
        user = userRepository.save(user);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        // Set OTP in Redis
        String otp = "654321";
        redisTemplate.opsForValue().set(OTP_PREFIX + "phone:" + user.getPhone(), otp, Duration.ofMinutes(5));

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setOtp(otp);

        mockMvc.perform(
                post("/api/user/verify/phone")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<UserResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertTrue(response.getData().getPhoneVerified());
        });
    }

    // ==================== ADMIN: GET ALL USERS ====================

    @Test
    void adminGetAllUsersSuccess() throws Exception {
        // Create admin and some users
        User admin = TestHelper.createAdminUser(userRepository);
        TestHelper.createTestUser(userRepository, "user1@example.com", "User One", Role.USER);
        TestHelper.createTestUser(userRepository, "user2@example.com", "User Two", Role.USER);

        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                get("/api/admin/users")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk(),
                jsonPath("$.data").isArray(),
                jsonPath("$.data").isNotEmpty()
        );
    }

    @Test
    void adminGetAllUsersForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        mockMvc.perform(
                get("/api/admin/users")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isForbidden()
        );
    }

    @Test
    void adminGetAllUsersWithSearch() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        TestHelper.createTestUser(userRepository, "john@example.com", "John Doe", Role.USER);
        TestHelper.createTestUser(userRepository, "jane@example.com", "Jane Smith", Role.USER);

        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                get("/api/admin/users")
                        .param("search", "john")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );
    }

    // ==================== ADMIN: GET USER BY ID ====================

    @Test
    void adminGetUserByIdSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        User targetUser = TestHelper.createTestUser(userRepository);

        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                get("/api/admin/users/" + targetUser.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<UserResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(targetUser.getEmail(), response.getData().getEmail());
        });
    }

    @Test
    void adminGetUserByIdNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                get("/api/admin/users/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    @Test
    void adminGetUserByIdForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        mockMvc.perform(
                get("/api/admin/users/" + user.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isForbidden()
        );
    }

    // ==================== ADMIN: UPDATE USER ====================

    @Test
    void adminUpdateUserRoleSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        User targetUser = TestHelper.createTestUser(userRepository);

        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setRole(Role.ADMIN);

        mockMvc.perform(
                patch("/api/admin/users/" + targetUser.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<UserResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(Role.ADMIN.name(), response.getData().getRole());

            // Verify in database
            User updated = userRepository.findById(targetUser.getId()).orElseThrow();
            assertEquals(Role.ADMIN, updated.getRole());
        });
    }

    @Test
    void adminUpdateUserStatusSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        User targetUser = TestHelper.createTestUser(userRepository);

        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setStatus(UserStatus.SUSPENDED);

        mockMvc.perform(
                patch("/api/admin/users/" + targetUser.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<UserResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(UserStatus.SUSPENDED, response.getData().getStatus());
        });
    }

    @Test
    void adminUpdateUserForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setRole(Role.ADMIN);

        mockMvc.perform(
                patch("/api/admin/users/" + user.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden()
        );
    }

    // ==================== ADMIN: DELETE USER ====================

    @Test
    void adminDeleteUserSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        User targetUser = TestHelper.createTestUser(userRepository);

        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                delete("/api/admin/users/" + targetUser.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );

        // Verify user was deleted
        assertFalse(userRepository.existsById(targetUser.getId()));
    }

    @Test
    void adminDeleteUserNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                delete("/api/admin/users/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    @Test
    void adminDeleteUserForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        User targetUser = TestHelper.createTestUser(userRepository, "target@example.com", "Target", Role.USER);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        mockMvc.perform(
                delete("/api/admin/users/" + targetUser.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isForbidden()
        );
    }
}

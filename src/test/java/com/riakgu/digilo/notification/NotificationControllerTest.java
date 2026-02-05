package com.riakgu.digilo.notification;

import com.riakgu.digilo.TestDataFactory;
import com.riakgu.digilo.TestHelper;
import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.config.TestMockConfig;
import com.riakgu.digilo.notification.dto.NotificationResponse;
import com.riakgu.digilo.notification.dto.UnreadCountResponse;
import com.riakgu.digilo.user.User;
import com.riakgu.digilo.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@ActiveProfiles("test")
@Import(TestMockConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== GET UNREAD COUNT ====================

    @Test
    void getUnreadCountSuccess() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        // Create unread notifications
        notificationRepository.save(TestDataFactory.notificationBuilder(user).isRead(false).build());
        notificationRepository.save(TestDataFactory.notificationBuilder(user).isRead(false).build());
        notificationRepository.save(TestDataFactory.notificationBuilder(user).isRead(true).build());

        mockMvc.perform(
                get("/api/user/notifications/unread-count")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<UnreadCountResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(2, response.getData().getCount());
        });
    }

    // ==================== MARK AS READ ====================

    @Test
    void markAsReadSuccess() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Notification notification = notificationRepository.save(
                TestDataFactory.notificationBuilder(user).isRead(false).build()
        );

        mockMvc.perform(
                patch("/api/user/notifications/" + notification.getId() + "/read")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );

        // Verify via database
        Notification updated = notificationRepository.findById(notification.getId()).orElseThrow();
        assertTrue(updated.getIsRead());
    }

    @Test
    void markAsReadNotFound() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        mockMvc.perform(
                patch("/api/user/notifications/999999/read")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== MARK ALL AS READ ====================

    @Test
    void markAllAsReadSuccess() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        // Create unread notifications
        notificationRepository.save(TestDataFactory.notificationBuilder(user).isRead(false).build());
        notificationRepository.save(TestDataFactory.notificationBuilder(user).isRead(false).build());

        mockMvc.perform(
                patch("/api/user/notifications/read-all")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );

        // Verify all are read
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
        assertEquals(0, unreadCount);
    }

    // ==================== UNAUTHORIZED ====================

    @Test
    void getUnreadCountUnauthorized() throws Exception {
        mockMvc.perform(
                get("/api/user/notifications/unread-count")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isUnauthorized()
        );
    }
}

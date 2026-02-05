package com.riakgu.digilo.order;

import com.riakgu.digilo.TestDataFactory;
import com.riakgu.digilo.TestHelper;
import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.config.TestMockConfig;
import com.riakgu.digilo.order.dto.OrderResponse;
import com.riakgu.digilo.order.dto.UpdateOrderStatusRequest;
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

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@ActiveProfiles("test")
@Import(TestMockConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== USER: GET ORDERS ====================

    @Test
    void getMyOrdersSuccess() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        // Create order for user
        orderRepository.save(TestDataFactory.orderBuilder(user)
                .status(OrderStatus.PENDING)
                .build());

        mockMvc.perform(
                get("/api/user/orders")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );
    }

    @Test
    void getOrderByIdSuccess() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Order order = orderRepository.save(TestDataFactory.orderBuilder(user)
                .status(OrderStatus.PENDING)
                .build());

        mockMvc.perform(
                get("/api/user/orders/" + order.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<OrderResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(order.getId(), response.getData().getId());
        });
    }

    @Test
    void getOrderByIdNotFound() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        mockMvc.perform(
                get("/api/user/orders/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    @Test
    void cancelOrderSuccess() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Order order = orderRepository.save(TestDataFactory.orderBuilder(user)
                .status(OrderStatus.PENDING)
                .build());

        mockMvc.perform(
                post("/api/user/orders/" + order.getId() + "/cancel")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<OrderResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals("CANCELLED", response.getData().getStatus().toString());
        });
    }

    // ==================== ADMIN: GET ORDER BY ID ====================

    @Test
    void adminGetOrderByIdSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        User user = TestHelper.createTestUser(userRepository);
        Order order = orderRepository.save(TestDataFactory.orderBuilder(user)
                .status(OrderStatus.PENDING)
                .build());

        mockMvc.perform(
                get("/api/admin/orders/" + order.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<OrderResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(order.getId(), response.getData().getId());
        });
    }

    @Test
    void adminGetOrderByIdForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Order order = orderRepository.save(TestDataFactory.orderBuilder(user)
                .status(OrderStatus.PENDING)
                .build());

        mockMvc.perform(
                get("/api/admin/orders/" + order.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isForbidden()
        );
    }

    // ==================== ADMIN: UPDATE STATUS ====================

    @Test
    void adminUpdateOrderStatusSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        User user = TestHelper.createTestUser(userRepository);
        Order order = orderRepository.save(TestDataFactory.orderBuilder(user)
                .status(OrderStatus.PAID)
                .build());

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.COMPLETED);

        mockMvc.perform(
                patch("/api/admin/orders/" + order.getId() + "/status")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<OrderResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals("COMPLETED", response.getData().getStatus().toString());
        });
    }

    @Test
    void adminUpdateOrderStatusNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.COMPLETED);

        mockMvc.perform(
                patch("/api/admin/orders/999999/status")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        );
    }
}

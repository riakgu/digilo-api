package com.riakgu.digilo.payment;

import com.riakgu.digilo.TestDataFactory;
import com.riakgu.digilo.TestHelper;
import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.config.TestMockConfig;
import com.riakgu.digilo.order.Order;
import com.riakgu.digilo.order.OrderRepository;
import com.riakgu.digilo.order.OrderStatus;
import com.riakgu.digilo.payment.dto.PaymentResponse;
import com.riakgu.digilo.payment.dto.RefundPaymentRequest;
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
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@ActiveProfiles("test")
@Import(TestMockConfig.class)
@Transactional
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Payment createPayment(Order order, PaymentStatus status) {
        return paymentRepository.save(
                Payment.builder()
                        .order(order)
                        .providerOrderId("MIDTRANS-" + UUID.randomUUID().toString().substring(0, 8))
                        .provider("MIDTRANS")
                        .currency("IDR")
                        .amount(new BigDecimal("100000.00"))
                        .status(status)
                        .paymentType("qris")
                        .expiredAt(Instant.now().plusSeconds(900))
                        .build()
        );
    }

    // ==================== USER: GET PAYMENT BY ID ====================

    @Test
    void getPaymentByIdSuccess() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Order order = orderRepository.save(TestDataFactory.orderBuilder(user).build());
        Payment payment = createPayment(order, PaymentStatus.PENDING);

        mockMvc.perform(
                get("/api/user/payments/" + payment.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<PaymentResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(payment.getId(), response.getData().getId());
        });
    }

    @Test
    void getPaymentByIdNotFound() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        mockMvc.perform(
                get("/api/user/payments/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== USER: GET PAYMENT BY ORDER ====================

    @Test
    void getPaymentByOrderIdSuccess() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Order order = orderRepository.save(TestDataFactory.orderBuilder(user).build());
        Payment payment = createPayment(order, PaymentStatus.PENDING);

        mockMvc.perform(
                get("/api/user/orders/" + order.getId() + "/payment")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<PaymentResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(payment.getId(), response.getData().getId());
        });
    }

    // ==================== ADMIN: GET PAYMENT BY ID ====================

    @Test
    void adminGetPaymentByIdSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        User user = TestHelper.createTestUser(userRepository);
        Order order = orderRepository.save(TestDataFactory.orderBuilder(user).build());
        Payment payment = createPayment(order, PaymentStatus.PENDING);

        mockMvc.perform(
                get("/api/admin/payments/" + payment.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<PaymentResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(payment.getId(), response.getData().getId());
        });
    }

    @Test
    void adminGetPaymentByIdForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Order order = orderRepository.save(TestDataFactory.orderBuilder(user).build());
        Payment payment = createPayment(order, PaymentStatus.PENDING);

        mockMvc.perform(
                get("/api/admin/payments/" + payment.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isForbidden()
        );
    }

    // ==================== ADMIN: CANCEL PAYMENT ====================

    @Test
    void adminCancelPaymentSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        User user = TestHelper.createTestUser(userRepository);
        Order order = orderRepository.save(TestDataFactory.orderBuilder(user).build());
        Payment payment = createPayment(order, PaymentStatus.PENDING);

        mockMvc.perform(
                post("/api/admin/payments/" + payment.getId() + "/cancel")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<PaymentResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals("CANCELLED", response.getData().getStatus().toString());
        });
    }

    @Test
    void adminCancelPaymentNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                post("/api/admin/payments/999999/cancel")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== ADMIN: REFUND PAYMENT ====================

    @Test
    void adminRefundPaymentSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        User user = TestHelper.createTestUser(userRepository);
        Order order = orderRepository.save(TestDataFactory.orderBuilder(user)
                .status(OrderStatus.PAID)
                .build());
        Payment payment = paymentRepository.save(
                Payment.builder()
                        .order(order)
                        .providerOrderId("MIDTRANS-" + UUID.randomUUID().toString().substring(0, 8))
                        .provider("MIDTRANS")
                        .currency("IDR")
                        .amount(new BigDecimal("100000.00"))
                        .status(PaymentStatus.SUCCESS)
                        .paymentType("qris")
                        .paidAt(Instant.now())
                        .build()
        );

        RefundPaymentRequest request = new RefundPaymentRequest();
        request.setNotes("Customer requested refund");

        mockMvc.perform(
                post("/api/admin/payments/" + payment.getId() + "/refund")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<PaymentResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals("REFUNDED", response.getData().getStatus().toString());
        });
    }
}


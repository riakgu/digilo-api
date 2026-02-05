package com.riakgu.digilo.dashboard;

import com.riakgu.digilo.TestHelper;
import com.riakgu.digilo.config.TestMockConfig;
import com.riakgu.digilo.config.TestContainersConfig;
import com.riakgu.digilo.order.OrderItemRepository;
import com.riakgu.digilo.order.OrderRepository;
import com.riakgu.digilo.payment.PaymentRepository;
import com.riakgu.digilo.product.ProductInventoryRepository;
import com.riakgu.digilo.product.ProductRepository;
import com.riakgu.digilo.product.ProductVariantRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@ActiveProfiles("test")
@Import({TestContainersConfig.class, TestMockConfig.class})
@Transactional
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductInventoryRepository productInventoryRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productInventoryRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== GET STATS ====================

    @Test
    void getStatsSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                get("/api/admin/dashboard/stats")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );
    }

    // ==================== GET TOP USERS ====================

    @Test
    void getTopUsersSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                get("/api/admin/dashboard/top-users")
                        .param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );
    }

    // ==================== GET TOP PRODUCTS ====================

    @Test
    void getTopProductsSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                get("/api/admin/dashboard/top-products")
                        .param("limit", "5")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );
    }

    // ==================== GET RECENT ORDERS ====================

    @Test
    void getRecentOrdersSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                get("/api/admin/dashboard/recent-orders")
                        .param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );
    }

    // ==================== GET SALES CHART ====================

    @Test
    void getSalesChartSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                get("/api/admin/dashboard/sales-chart")
                        .param("period", "7d")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );
    }

    // ==================== FORBIDDEN AS USER ====================

    @Test
    void getStatsForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        mockMvc.perform(
                get("/api/admin/dashboard/stats")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isForbidden()
        );
    }
}

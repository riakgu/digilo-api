package com.riakgu.digilo.promo;

import com.riakgu.digilo.TestDataFactory;
import com.riakgu.digilo.TestHelper;
import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.config.TestMockConfig;
import com.riakgu.digilo.promo.dto.ApplyPromoRequest;
import com.riakgu.digilo.promo.dto.PromoRequest;
import com.riakgu.digilo.promo.dto.PromoResponse;
import com.riakgu.digilo.promo.dto.PromoValidationResponse;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@ActiveProfiles("test")
@Import(TestMockConfig.class)
@Transactional
class PromoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PromoRepository promoRepository;

    @Autowired
    private com.riakgu.digilo.cart.CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
        promoRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== USER: VALIDATE PROMO ====================

    @Test
    void validatePromoSuccess() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        // Create cart (required for promo validation)
        cartRepository.save(com.riakgu.digilo.cart.Cart.builder().user(user).build());

        // Create active promo
        promoRepository.save(TestDataFactory.promoBuilder()
                .code("TESTCODE")
                .isActive(true)
                .startsAt(Instant.now().minusSeconds(86400))
                .expiresAt(Instant.now().plusSeconds(86400))
                .build());

        ApplyPromoRequest request = new ApplyPromoRequest();
        request.setCode("TESTCODE");

        mockMvc.perform(
                post("/api/user/promos/validate")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<PromoValidationResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertNotNull(response.getData());
        });
    }

    @Test
    void validatePromoCodeNotFound() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        ApplyPromoRequest request = new ApplyPromoRequest();
        request.setCode("INVALIDCODE");

        mockMvc.perform(
                post("/api/user/promos/validate")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== ADMIN: CREATE PROMO ====================

    @Test
    void adminCreatePromoSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        PromoRequest request = new PromoRequest();
        request.setCode("NEWPROMO");
        request.setName("New Promo");
        request.setDiscountType(DiscountType.PERCENT);
        request.setDiscountValue(BigDecimal.valueOf(10));

        mockMvc.perform(
                post("/api/admin/promos")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        ).andDo(result -> {
            ApiResponse<PromoResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals("NEWPROMO", response.getData().getCode());
        });
    }

    @Test
    void adminCreatePromoBadRequest() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        PromoRequest request = new PromoRequest();
        // Missing required fields

        mockMvc.perform(
                post("/api/admin/promos")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        );
    }

    @Test
    void adminCreatePromoForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        PromoRequest request = new PromoRequest();
        request.setCode("PROMO");
        request.setName("Promo");
        request.setDiscountType(DiscountType.PERCENT);
        request.setDiscountValue(BigDecimal.valueOf(10));

        mockMvc.perform(
                post("/api/admin/promos")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden()
        );
    }

    // ==================== ADMIN: GET BY ID ====================

    @Test
    void adminGetByIdSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Promo promo = promoRepository.save(TestDataFactory.buildPromo());

        mockMvc.perform(
                get("/api/admin/promos/" + promo.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<PromoResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(promo.getId(), response.getData().getId());
        });
    }

    @Test
    void adminGetByIdNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                get("/api/admin/promos/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== ADMIN: UPDATE ====================

    @Test
    void adminUpdatePromoSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Promo promo = promoRepository.save(TestDataFactory.promoBuilder()
                .code("OLDCODE")
                .build());

        PromoRequest request = new PromoRequest();
        request.setCode("NEWCODE");
        request.setName("Updated Promo");
        request.setDiscountType(DiscountType.FIXED);
        request.setDiscountValue(BigDecimal.valueOf(50000));

        mockMvc.perform(
                put("/api/admin/promos/" + promo.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<PromoResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals("NEWCODE", response.getData().getCode());
        });
    }

    // ==================== ADMIN: DELETE ====================

    @Test
    void adminDeletePromoSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Promo promo = promoRepository.save(TestDataFactory.buildPromo());

        mockMvc.perform(
                delete("/api/admin/promos/" + promo.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );

        // Verify deleted
        assertFalse(promoRepository.existsById(promo.getId()));
    }
}

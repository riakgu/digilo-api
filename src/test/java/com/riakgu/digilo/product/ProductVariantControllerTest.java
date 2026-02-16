package com.riakgu.digilo.product;

import com.riakgu.digilo.TestDataFactory;
import com.riakgu.digilo.TestHelper;
import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.config.TestMockConfig;
import com.riakgu.digilo.config.TestContainersConfig;
import com.riakgu.digilo.product.variant.DeliveryType;
import com.riakgu.digilo.product.variant.dto.ProductVariantRequest;
import com.riakgu.digilo.product.variant.dto.ProductVariantResponse;
import com.riakgu.digilo.product.variant.ProductVariant;
import com.riakgu.digilo.product.variant.ProductVariantRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@ActiveProfiles("test")
@Import({TestContainersConfig.class, TestMockConfig.class})
@Transactional
class ProductVariantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        variantRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== PUBLIC: GET ACTIVE VARIANTS ====================

    @Test
    void getActiveVariantsByProductSuccess() throws Exception {
        Product product = productRepository.save(TestDataFactory.productBuilder().isActive(true).build());

        ProductVariant variant = TestDataFactory.variantBuilder(product)
                .isActive(true)
                .build();
        variantRepository.save(variant);

        mockMvc.perform(
                get("/api/public/products/" + product.getId() + "/variants")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<List<ProductVariantResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertNotNull(response.getData());
        });
    }

    @Test
    void getActiveVariantsByProductNotFound() throws Exception {
        mockMvc.perform(
                get("/api/public/products/999999/variants")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== ADMIN: CREATE VARIANT ====================

    @Test
    void adminCreateVariantSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());

        ProductVariantRequest request = new ProductVariantRequest();
        request.setSku("SKU-001");
        request.setName("Standard Variant");
        request.setPrice(BigDecimal.valueOf(99.99));
        request.setDeliveryType(DeliveryType.AUTO);

        mockMvc.perform(
                post("/api/admin/products/" + product.getId() + "/variants")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        ).andDo(result -> {
            ApiResponse<ProductVariantResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals("SKU-001", response.getData().getSku());
            assertEquals("Standard Variant", response.getData().getName());
        });
    }

    @Test
    void adminCreateVariantBadRequestEmptyFields() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());

        ProductVariantRequest request = new ProductVariantRequest();
        request.setSku("");
        request.setName("");

        mockMvc.perform(
                post("/api/admin/products/" + product.getId() + "/variants")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        );
    }

    @Test
    void adminCreateVariantProductNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        ProductVariantRequest request = new ProductVariantRequest();
        request.setSku("SKU-001");
        request.setName("Standard Variant");
        request.setPrice(BigDecimal.valueOf(99.99));
        request.setDeliveryType(DeliveryType.AUTO);

        mockMvc.perform(
                post("/api/admin/products/999999/variants")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        );
    }

    @Test
    void adminCreateVariantForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());

        ProductVariantRequest request = new ProductVariantRequest();
        request.setSku("SKU-001");
        request.setName("Standard Variant");
        request.setPrice(BigDecimal.valueOf(99.99));
        request.setDeliveryType(DeliveryType.AUTO);

        mockMvc.perform(
                post("/api/admin/products/" + product.getId() + "/variants")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden()
        );
    }

    // ==================== ADMIN: GET VARIANTS BY PRODUCT ====================

    @Test
    void adminGetVariantsByProductSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        variantRepository.save(TestDataFactory.variantBuilder(product).build());

        mockMvc.perform(
                get("/api/admin/products/" + product.getId() + "/variants")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<List<ProductVariantResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertNotNull(response.getData());
        });
    }

    // ==================== ADMIN: GET VARIANT BY ID ====================

    @Test
    void adminGetVariantByIdSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(
                TestDataFactory.variantBuilder(product).build()
        );

        mockMvc.perform(
                get("/api/admin/variants/" + variant.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<ProductVariantResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(variant.getId(), response.getData().getId());
        });
    }

    @Test
    void adminGetVariantByIdNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                get("/api/admin/variants/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== ADMIN: UPDATE VARIANT ====================

    @Test
    void adminUpdateVariantSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(
                TestDataFactory.variantBuilder(product).sku("OLD-SKU").name("Old Name").build()
        );

        ProductVariantRequest request = new ProductVariantRequest();
        request.setSku("NEW-SKU");
        request.setName("New Name");
        request.setPrice(BigDecimal.valueOf(199.99));
        request.setDeliveryType(DeliveryType.AUTO);

        mockMvc.perform(
                put("/api/admin/variants/" + variant.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<ProductVariantResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals("NEW-SKU", response.getData().getSku());
            assertEquals("New Name", response.getData().getName());
        });
    }

    @Test
    void adminUpdateVariantNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        ProductVariantRequest request = new ProductVariantRequest();
        request.setSku("NEW-SKU");
        request.setName("New Name");
        request.setPrice(BigDecimal.valueOf(199.99));
        request.setDeliveryType(DeliveryType.AUTO);

        mockMvc.perform(
                put("/api/admin/variants/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        );
    }

    @Test
    void adminUpdateVariantForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(
                TestDataFactory.variantBuilder(product).build()
        );

        ProductVariantRequest request = new ProductVariantRequest();
        request.setSku("NEW-SKU");
        request.setName("New Name");
        request.setPrice(BigDecimal.valueOf(199.99));
        request.setDeliveryType(DeliveryType.AUTO);

        mockMvc.perform(
                put("/api/admin/variants/" + variant.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden()
        );
    }

    // ==================== ADMIN: DELETE VARIANT ====================

    @Test
    void adminDeleteVariantSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(
                TestDataFactory.variantBuilder(product).build()
        );

        mockMvc.perform(
                delete("/api/admin/variants/" + variant.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );

        assertFalse(variantRepository.existsById(variant.getId()));
    }

    @Test
    void adminDeleteVariantNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                delete("/api/admin/variants/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    @Test
    void adminDeleteVariantForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(
                TestDataFactory.variantBuilder(product).build()
        );

        mockMvc.perform(
                delete("/api/admin/variants/" + variant.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isForbidden()
        );
    }
}

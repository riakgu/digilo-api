package com.riakgu.digilo.product;

import com.riakgu.digilo.TestDataFactory;
import com.riakgu.digilo.TestHelper;
import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.config.TestMockConfig;
import com.riakgu.digilo.config.TestContainersConfig;
import com.riakgu.digilo.product.inventory.InventoryStatus;
import com.riakgu.digilo.product.inventory.dto.ProductInventoryBulkRequest;
import com.riakgu.digilo.product.inventory.dto.ProductInventoryRequest;
import com.riakgu.digilo.product.inventory.dto.ProductInventoryResponse;
import com.riakgu.digilo.product.inventory.dto.ProductInventoryUpdateRequest;
import com.riakgu.digilo.product.inventory.ProductInventory;
import com.riakgu.digilo.product.inventory.ProductInventoryRepository;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@ActiveProfiles("test")
@Import({TestContainersConfig.class, TestMockConfig.class})
@Transactional
class ProductInventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private ProductInventoryRepository inventoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        variantRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== PUBLIC: GET STOCK ====================

    @Test
    void getPublicStockSuccess() throws Exception {
        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(TestDataFactory.variantBuilder(product).build());

        inventoryRepository.save(TestDataFactory.inventoryBuilder(variant).status(InventoryStatus.AVAILABLE).build());
        inventoryRepository.save(TestDataFactory.inventoryBuilder(variant).status(InventoryStatus.AVAILABLE).build());

        mockMvc.perform(
                get("/api/public/variants/" + variant.getId() + "/stock")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<Long> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(2L, response.getData());
        });
    }

    // ==================== ADMIN: CREATE INVENTORY ====================

    @Test
    void adminCreateInventorySuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(TestDataFactory.variantBuilder(product).build());

        ProductInventoryRequest request = new ProductInventoryRequest();
        request.setVariantId(variant.getId());
        request.setCredential(Map.of("key", "secret-value-123"));

        mockMvc.perform(
                post("/api/admin/inventories")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        ).andDo(result -> {
            ApiResponse<ProductInventoryResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertNotNull(response.getData().getId());
        });
    }

    @Test
    void adminCreateInventoryBadRequest() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        ProductInventoryRequest request = new ProductInventoryRequest();

        mockMvc.perform(
                post("/api/admin/inventories")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        );
    }

    @Test
    void adminCreateInventoryForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(TestDataFactory.variantBuilder(product).build());

        ProductInventoryRequest request = new ProductInventoryRequest();
        request.setVariantId(variant.getId());
        request.setCredential(Map.of("key", "value"));

        mockMvc.perform(
                post("/api/admin/inventories")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden()
        );
    }

    // ==================== ADMIN: BULK CREATE ====================

    @Test
    void adminBulkCreateSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(TestDataFactory.variantBuilder(product).build());

        ProductInventoryBulkRequest request = new ProductInventoryBulkRequest();
        request.setVariantId(variant.getId());
        request.setCredentials(List.of(
                Map.of("key", "value1"),
                Map.of("key", "value2"),
                Map.of("key", "value3")
        ));

        mockMvc.perform(
                post("/api/admin/inventories/bulk")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        ).andDo(result -> {
            ApiResponse<List<ProductInventoryResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(3, response.getData().size());
        });
    }

    // ==================== ADMIN: GET INVENTORY ====================

    @Test
    void adminGetByIdSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(TestDataFactory.variantBuilder(product).build());
        ProductInventory inventory = inventoryRepository.save(TestDataFactory.inventoryBuilder(variant).build());

        mockMvc.perform(
                get("/api/admin/inventories/" + inventory.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<ProductInventoryResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(inventory.getId(), response.getData().getId());
        });
    }

    @Test
    void adminGetByIdNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                get("/api/admin/inventories/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    @Test
    void adminGetByIdWithCredentialSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(TestDataFactory.variantBuilder(product).build());
        ProductInventory inventory = inventoryRepository.save(TestDataFactory.inventoryBuilder(variant).build());

        mockMvc.perform(
                get("/api/admin/inventories/" + inventory.getId() + "/credential")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<ProductInventoryResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertNotNull(response.getData().getCredential());
        });
    }

    @Test
    void adminGetByVariantSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(TestDataFactory.variantBuilder(product).build());
        inventoryRepository.save(TestDataFactory.inventoryBuilder(variant).build());

        mockMvc.perform(
                get("/api/admin/variants/" + variant.getId() + "/inventories")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<List<ProductInventoryResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertFalse(response.getData().isEmpty());
        });
    }

    @Test
    void adminGetAvailableStockSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(TestDataFactory.variantBuilder(product).build());
        inventoryRepository.save(TestDataFactory.inventoryBuilder(variant).status(InventoryStatus.AVAILABLE).build());

        mockMvc.perform(
                get("/api/admin/variants/" + variant.getId() + "/stock")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<Long> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(1L, response.getData());
        });
    }

    // ==================== ADMIN: UPDATE ====================

    @Test
    void adminUpdateInventorySuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(TestDataFactory.variantBuilder(product).build());
        ProductInventory inventory = inventoryRepository.save(TestDataFactory.inventoryBuilder(variant).build());

        ProductInventoryUpdateRequest request = new ProductInventoryUpdateRequest();
        request.setVariantId(variant.getId());
        request.setCredential(Map.of("newKey", "newValue"));

        mockMvc.perform(
                put("/api/admin/inventories/" + inventory.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        );
    }

    @Test
    void adminUpdateInventoryNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(TestDataFactory.variantBuilder(product).build());

        ProductInventoryUpdateRequest request = new ProductInventoryUpdateRequest();
        request.setVariantId(variant.getId());
        request.setCredential(Map.of("key", "value"));

        mockMvc.perform(
                put("/api/admin/inventories/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== ADMIN: STATUS OPERATIONS ====================

    @Test
    void adminMarkAsSoldSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(TestDataFactory.variantBuilder(product).build());
        ProductInventory inventory = inventoryRepository.save(
                TestDataFactory.inventoryBuilder(variant).status(InventoryStatus.RESERVED).build()
        );

        mockMvc.perform(
                post("/api/admin/inventories/" + inventory.getId() + "/sold")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<ProductInventoryResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals("SOLD", response.getData().getStatus().toString());
        });
    }

    @Test
    void adminReleaseReservationSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(TestDataFactory.variantBuilder(product).build());
        ProductInventory inventory = inventoryRepository.save(
                TestDataFactory.inventoryBuilder(variant).status(InventoryStatus.RESERVED).build()
        );

        mockMvc.perform(
                post("/api/admin/inventories/" + inventory.getId() + "/release")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );

        ProductInventory updated = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertEquals(InventoryStatus.AVAILABLE, updated.getStatus());
    }

    // ==================== ADMIN: DELETE ====================

    @Test
    void adminDeleteInventorySuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(TestDataFactory.variantBuilder(product).build());
        ProductInventory inventory = inventoryRepository.save(TestDataFactory.inventoryBuilder(variant).build());

        mockMvc.perform(
                delete("/api/admin/inventories/" + inventory.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );

        assertFalse(inventoryRepository.existsById(inventory.getId()));
    }

    @Test
    void adminDeleteInventoryNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                delete("/api/admin/inventories/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isNotFound()
        );
    }
}

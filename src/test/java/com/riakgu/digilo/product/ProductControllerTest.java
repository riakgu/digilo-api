package com.riakgu.digilo.product;

import com.riakgu.digilo.TestDataFactory;
import com.riakgu.digilo.TestHelper;
import com.riakgu.digilo.category.Category;
import com.riakgu.digilo.category.CategoryRepository;
import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.config.TestMockConfig;
import com.riakgu.digilo.product.dto.ProductRequest;
import com.riakgu.digilo.product.dto.ProductResponse;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@ActiveProfiles("test")
@Import(TestMockConfig.class)
@Transactional
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== PUBLIC: GET BY SLUG ====================

    @Test
    void getBySlugSuccess() throws Exception {
        Product product = productRepository.save(
                TestDataFactory.productBuilder().name("Test Product").slug("test-product").isActive(true).build()
        );

        mockMvc.perform(
                get("/api/public/products/" + product.getSlug())
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<ProductResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals("test-product", response.getData().getSlug());
            assertEquals("Test Product", response.getData().getName());
        });
    }

    @Test
    void getBySlugNotFound() throws Exception {
        mockMvc.perform(
                get("/api/public/products/nonexistent")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    @Test
    void getBySlugNotFoundWhenInactive() throws Exception {
        productRepository.save(
                TestDataFactory.productBuilder().slug("inactive-product").isActive(false).build()
        );

        mockMvc.perform(
                get("/api/public/products/inactive-product")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== PUBLIC: GET ALL ACTIVE ====================

    @Test
    void getAllActiveSuccess() throws Exception {
        productRepository.save(TestDataFactory.productBuilder().isActive(true).build());
        productRepository.save(TestDataFactory.productBuilder().isActive(true).build());
        productRepository.save(TestDataFactory.productBuilder().isActive(false).build()); // Inactive

        mockMvc.perform(
                get("/api/public/products")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk(),
                jsonPath("$.data").isArray()
        );
    }

    // ==================== PUBLIC: SEARCH ====================

    @Test
    void searchSuccess() throws Exception {
        productRepository.save(
                TestDataFactory.productBuilder().name("Searchable Product").slug("searchable-product").isActive(true).build()
        );

        mockMvc.perform(
                get("/api/public/products/search")
                        .param("q", "Searchable")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk(),
                jsonPath("$.data").isArray()
        );
    }

    // ==================== PUBLIC: GET CATEGORIES BY PRODUCT ====================

    @Test
    void getCategoriesByProductSuccess() throws Exception {
        Category category = categoryRepository.save(TestDataFactory.buildCategory());
        Product product = productRepository.save(
                TestDataFactory.productBuilder().slug("product-with-category").isActive(true).build()
        );

        // Link product to category
        ProductCategory pc = new ProductCategory();
        pc.setId(new ProductCategoryId(product.getId(), category.getId()));
        pc.setProduct(product);
        pc.setCategory(category);
        product.getCategories().add(pc);
        productRepository.save(product);

        mockMvc.perform(
                get("/api/public/products/product-with-category/categories")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk(),
                jsonPath("$.data").isArray()
        );
    }

    @Test
    void getCategoriesByProductNotFound() throws Exception {
        mockMvc.perform(
                get("/api/public/products/nonexistent/categories")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== ADMIN: CREATE PRODUCT ====================

    @Test
    void adminCreateProductSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        ProductRequest request = new ProductRequest();
        request.setName("New Product");
        request.setSlug("new-product");
        request.setDescription("A new test product");

        mockMvc.perform(
                post("/api/admin/products")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        ).andDo(result -> {
            ApiResponse<ProductResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals("New Product", response.getData().getName());
            assertEquals("new-product", response.getData().getSlug());

            // Verify in database
            assertTrue(productRepository.existsBySlug("new-product"));
        });
    }

    @Test
    void adminCreateProductWithCategories() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Category category = categoryRepository.save(TestDataFactory.buildCategory());

        ProductRequest request = new ProductRequest();
        request.setName("Product With Category");
        request.setSlug("product-with-category");
        request.setCategoryIds(List.of(category.getId()));

        mockMvc.perform(
                post("/api/admin/products")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        );
    }

    @Test
    void adminCreateProductBadRequestEmptyFields() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        ProductRequest request = new ProductRequest();
        request.setName("");
        request.setSlug("");

        mockMvc.perform(
                post("/api/admin/products")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        );
    }

    @Test
    void adminCreateProductDuplicateSlug() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        // Create existing product
        productRepository.save(TestDataFactory.productBuilder().slug("existing-slug").build());

        ProductRequest request = new ProductRequest();
        request.setName("Another Product");
        request.setSlug("existing-slug");

        mockMvc.perform(
                post("/api/admin/products")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isConflict()
        );
    }

    @Test
    void adminCreateProductForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        ProductRequest request = new ProductRequest();
        request.setName("New Product");
        request.setSlug("new-product");

        mockMvc.perform(
                post("/api/admin/products")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden()
        );
    }

    // ==================== ADMIN: GET ALL (PAGINATED) ====================

    @Test
    void adminGetAllProductsSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        productRepository.save(TestDataFactory.buildProduct());
        productRepository.save(TestDataFactory.buildProduct());

        mockMvc.perform(
                get("/api/admin/products")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );
    }

    // ==================== ADMIN: GET BY ID ====================

    @Test
    void adminGetByIdSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());

        mockMvc.perform(
                get("/api/admin/products/" + product.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<ProductResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(product.getId(), response.getData().getId());
        });
    }

    @Test
    void adminGetByIdNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                get("/api/admin/products/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== ADMIN: UPDATE PRODUCT ====================

    @Test
    void adminUpdateProductSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(
                TestDataFactory.productBuilder().name("Old Name").slug("old-slug").build()
        );

        ProductRequest request = new ProductRequest();
        request.setName("Updated Name");
        request.setSlug("updated-slug");
        request.setDescription("Updated description");

        mockMvc.perform(
                put("/api/admin/products/" + product.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<ProductResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals("Updated Name", response.getData().getName());
            assertEquals("updated-slug", response.getData().getSlug());
        });
    }

    @Test
    void adminUpdateProductNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        ProductRequest request = new ProductRequest();
        request.setName("Updated Name");
        request.setSlug("updated-slug");

        mockMvc.perform(
                put("/api/admin/products/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        );
    }

    @Test
    void adminUpdateProductForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());

        ProductRequest request = new ProductRequest();
        request.setName("Updated Name");
        request.setSlug("updated-slug");

        mockMvc.perform(
                put("/api/admin/products/" + product.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden()
        );
    }

    // ==================== ADMIN: DELETE PRODUCT ====================

    @Test
    void adminDeleteProductSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());

        mockMvc.perform(
                delete("/api/admin/products/" + product.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );

        // Verify deleted
        assertFalse(productRepository.existsById(product.getId()));
    }

    @Test
    void adminDeleteProductNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                delete("/api/admin/products/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    @Test
    void adminDeleteProductForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());

        mockMvc.perform(
                delete("/api/admin/products/" + product.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isForbidden()
        );
    }
}

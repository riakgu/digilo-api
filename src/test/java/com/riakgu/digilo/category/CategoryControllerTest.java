package com.riakgu.digilo.category;

import com.riakgu.digilo.TestDataFactory;
import com.riakgu.digilo.TestHelper;
import com.riakgu.digilo.category.dto.CategoryRequest;
import com.riakgu.digilo.category.dto.CategoryResponse;
import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.config.TestMockConfig;
import com.riakgu.digilo.product.Product;
import com.riakgu.digilo.product.ProductCategory;
import com.riakgu.digilo.product.ProductCategoryId;
import com.riakgu.digilo.product.ProductRepository;
import com.riakgu.digilo.user.Role;
import com.riakgu.digilo.user.User;
import com.riakgu.digilo.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@ActiveProfiles("test")
@Import(TestMockConfig.class)
@Transactional
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

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
        Category category = TestDataFactory.categoryBuilder()
                .name("Gaming")
                .slug("gaming")
                .isActive(true)
                .build();
        category = categoryRepository.save(category);

        mockMvc.perform(
                get("/api/public/categories/" + category.getSlug())
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<CategoryResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals("gaming", response.getData().getSlug());
            assertEquals("Gaming", response.getData().getName());
        });
    }

    @Test
    void getBySlugNotFound() throws Exception {
        mockMvc.perform(
                get("/api/public/categories/nonexistent")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    @Test
    void getBySlugNotFoundWhenInactive() throws Exception {
        Category category = TestDataFactory.categoryBuilder()
                .slug("inactive-category")
                .isActive(false)
                .build();
        categoryRepository.save(category);

        mockMvc.perform(
                get("/api/public/categories/inactive-category")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== PUBLIC: GET ALL ACTIVE ====================

    @Test
    void getAllActiveSuccess() throws Exception {
        categoryRepository.save(TestDataFactory.categoryBuilder().isActive(true).build());
        categoryRepository.save(TestDataFactory.categoryBuilder().isActive(true).build());
        categoryRepository.save(TestDataFactory.categoryBuilder().isActive(false).build()); // Inactive

        mockMvc.perform(
                get("/api/public/categories")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk(),
                jsonPath("$.data").isArray()
        );
    }

    // ==================== PUBLIC: GET PRODUCTS BY CATEGORY ====================

    @Test
    void getProductsByCategorySuccess() throws Exception {
        Category category = categoryRepository.save(
                TestDataFactory.categoryBuilder().slug("software").isActive(true).build()
        );

        Product product = productRepository.save(TestDataFactory.productBuilder().isActive(true).build());
        ProductCategory pc = new ProductCategory();
        pc.setId(new ProductCategoryId(product.getId(), category.getId()));
        pc.setProduct(product);
        pc.setCategory(category);
        product.getCategories().add(pc);
        productRepository.save(product);

        mockMvc.perform(
                get("/api/public/categories/software/products")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk(),
                jsonPath("$.data").isArray()
        );
    }

    @Test
    void getProductsByCategoryNotFound() throws Exception {
        mockMvc.perform(
                get("/api/public/categories/nonexistent/products")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== ADMIN: CREATE CATEGORY ====================

    @Test
    void adminCreateCategorySuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        CategoryRequest request = new CategoryRequest();
        request.setName("New Category");
        request.setSlug("new-category");
        request.setDescription("A new test category");

        mockMvc.perform(
                post("/api/admin/categories")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        ).andDo(result -> {
            ApiResponse<CategoryResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals("New Category", response.getData().getName());
            assertEquals("new-category", response.getData().getSlug());

            // Verify in database
            assertTrue(categoryRepository.existsBySlug("new-category"));
        });
    }

    @Test
    void adminCreateCategoryBadRequestEmptyFields() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        CategoryRequest request = new CategoryRequest();
        request.setName("");
        request.setSlug("");

        mockMvc.perform(
                post("/api/admin/categories")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        );
    }

    @Test
    void adminCreateCategoryDuplicateSlug() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        // Create existing category
        categoryRepository.save(TestDataFactory.categoryBuilder().slug("existing-slug").build());

        CategoryRequest request = new CategoryRequest();
        request.setName("Another Category");
        request.setSlug("existing-slug");

        mockMvc.perform(
                post("/api/admin/categories")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isConflict()
        );
    }

    @Test
    void adminCreateCategoryForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        CategoryRequest request = new CategoryRequest();
        request.setName("New Category");
        request.setSlug("new-category");

        mockMvc.perform(
                post("/api/admin/categories")
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
    void adminGetAllCategoriesSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        categoryRepository.save(TestDataFactory.buildCategory());
        categoryRepository.save(TestDataFactory.buildCategory());

        mockMvc.perform(
                get("/api/admin/categories")
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

        Category category = categoryRepository.save(TestDataFactory.buildCategory());

        mockMvc.perform(
                get("/api/admin/categories/" + category.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<CategoryResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(category.getId(), response.getData().getId());
        });
    }

    @Test
    void adminGetByIdNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                get("/api/admin/categories/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== ADMIN: UPDATE CATEGORY ====================

    @Test
    void adminUpdateCategorySuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Category category = categoryRepository.save(
                TestDataFactory.categoryBuilder().name("Old Name").slug("old-slug").build()
        );

        CategoryRequest request = new CategoryRequest();
        request.setName("Updated Name");
        request.setSlug("updated-slug");
        request.setDescription("Updated description");

        mockMvc.perform(
                put("/api/admin/categories/" + category.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<CategoryResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals("Updated Name", response.getData().getName());
            assertEquals("updated-slug", response.getData().getSlug());

            // Verify in database
            Category updated = categoryRepository.findById(category.getId()).orElseThrow();
            assertEquals("Updated Name", updated.getName());
        });
    }

    @Test
    void adminUpdateCategoryNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        CategoryRequest request = new CategoryRequest();
        request.setName("Updated Name");
        request.setSlug("updated-slug");

        mockMvc.perform(
                put("/api/admin/categories/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        );
    }

    @Test
    void adminUpdateCategoryForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Category category = categoryRepository.save(TestDataFactory.buildCategory());

        CategoryRequest request = new CategoryRequest();
        request.setName("Updated Name");
        request.setSlug("updated-slug");

        mockMvc.perform(
                put("/api/admin/categories/" + category.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden()
        );
    }

    // ==================== ADMIN: DELETE CATEGORY ====================

    @Test
    @Disabled
    void adminDeleteCategorySuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Category category = categoryRepository.save(TestDataFactory.buildCategory());

        mockMvc.perform(
                delete("/api/admin/categories/" + category.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );

        // Verify deleted
        assertFalse(categoryRepository.existsById(category.getId()));
    }

    @Test
    void adminDeleteCategoryNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        mockMvc.perform(
                delete("/api/admin/categories/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    @Test
    void adminDeleteCategoryForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Category category = categoryRepository.save(TestDataFactory.buildCategory());

        mockMvc.perform(
                delete("/api/admin/categories/" + category.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isForbidden()
        );
    }

    @Test
    @Disabled
    void adminDeleteCategoryBadRequestWithProducts() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Category category = categoryRepository.save(TestDataFactory.buildCategory());

        // Add a product to the category
        Product product = productRepository.save(TestDataFactory.productBuilder().build());
        ProductCategory pc = new ProductCategory();
        pc.setId(new ProductCategoryId(product.getId(), category.getId()));
        pc.setProduct(product);
        pc.setCategory(category);
        product.getCategories().add(pc);
        productRepository.save(product);

        mockMvc.perform(
                delete("/api/admin/categories/" + category.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isBadRequest()
        );

        // Verify not deleted
        assertTrue(categoryRepository.existsById(category.getId()));
    }
}

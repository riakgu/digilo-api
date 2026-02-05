package com.riakgu.digilo.product;

import com.riakgu.digilo.TestDataFactory;
import com.riakgu.digilo.TestHelper;
import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.config.TestMockConfig;
import com.riakgu.digilo.product.dto.ProductImageRequest;
import com.riakgu.digilo.product.dto.ProductImageResponse;
import com.riakgu.digilo.product.dto.ReorderImagesRequest;
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
import org.springframework.mock.web.MockMultipartFile;
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
class ProductImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        imageRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== PUBLIC: GET IMAGES ====================

    @Test
    @Disabled
    void getImagesSuccess() throws Exception {
        Product product = productRepository.save(TestDataFactory.buildProduct());

        // Add images
        ProductImage image1 = ProductImage.builder()
                .product(product)
                .imageUrl("https://example.com/image1.jpg")
                .displayOrder(0)
                .isPrimary(true)
                .build();
        ProductImage image2 = ProductImage.builder()
                .product(product)
                .imageUrl("https://example.com/image2.jpg")
                .displayOrder(1)
                .isPrimary(false)
                .build();
        imageRepository.saveAll(List.of(image1, image2));

        mockMvc.perform(
                get("/api/public/products/" + product.getId() + "/images")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<List<ProductImageResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(2, response.getData().size());
        });
    }

    // ==================== ADMIN: ADD IMAGE ====================

    @Test
    void adminAddImageSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());

        ProductImageRequest request = new ProductImageRequest();
        request.setImageUrl("https://example.com/new-image.jpg");
        request.setIsPrimary(false);

        mockMvc.perform(
                post("/api/admin/products/" + product.getId() + "/images")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        );

        // Verify image added
        List<ProductImage> images = imageRepository.findAllByProductId(product.getId());
        assertFalse(images.isEmpty());
    }

    @Test
    void adminAddImageBadRequest() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());

        ProductImageRequest request = new ProductImageRequest();
        // Missing imageUrl

        mockMvc.perform(
                post("/api/admin/products/" + product.getId() + "/images")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        );
    }

    @Test
    void adminAddImageForbiddenAsUser() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());

        ProductImageRequest request = new ProductImageRequest();
        request.setImageUrl("https://example.com/image.jpg");

        mockMvc.perform(
                post("/api/admin/products/" + product.getId() + "/images")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden()
        );
    }

    // ==================== ADMIN: UPLOAD IMAGE ====================

    @Test
    void adminUploadImageSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        mockMvc.perform(
                multipart("/api/admin/products/" + product.getId() + "/images/upload")
                        .file(file)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<String> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertNotNull(response.getData());
        });
    }

    // ==================== ADMIN: GET IMAGE BY ID ====================

    @Test
    void adminGetImageByIdSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductImage image = imageRepository.save(
                ProductImage.builder()
                        .product(product)
                        .imageUrl("https://example.com/image.jpg")
                        .displayOrder(0)
                        .isPrimary(false)
                        .build()
        );

        mockMvc.perform(
                get("/api/admin/products/" + product.getId() + "/images/" + image.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<ProductImageResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertEquals(image.getId(), response.getData().getId());
        });
    }

    @Test
    void adminGetImageByIdNotFound() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());

        mockMvc.perform(
                get("/api/admin/products/" + product.getId() + "/images/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== ADMIN: UPDATE IMAGE ====================

    @Test
    void adminUpdateImageSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductImage image = imageRepository.save(
                ProductImage.builder()
                        .product(product)
                        .imageUrl("https://example.com/old-image.jpg")
                        .displayOrder(0)
                        .isPrimary(false)
                        .build()
        );

        ProductImageRequest request = new ProductImageRequest();
        request.setImageUrl("https://example.com/new-image.jpg");
        request.setIsPrimary(true);

        mockMvc.perform(
                put("/api/admin/products/" + product.getId() + "/images/" + image.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        );
    }

    // ==================== ADMIN: DELETE IMAGE ====================

    @Test
    void adminDeleteImageSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductImage image = imageRepository.save(
                ProductImage.builder()
                        .product(product)
                        .imageUrl("https://example.com/image.jpg")
                        .displayOrder(0)
                        .isPrimary(false)
                        .build()
        );

        mockMvc.perform(
                delete("/api/admin/products/" + product.getId() + "/images/" + image.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );

        // Verify deleted
        assertFalse(imageRepository.existsById(image.getId()));
    }

    // ==================== ADMIN: SET PRIMARY ====================

    @Test
    void adminSetPrimaryImageSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductImage image = imageRepository.save(
                ProductImage.builder()
                        .product(product)
                        .imageUrl("https://example.com/image.jpg")
                        .displayOrder(0)
                        .isPrimary(false)
                        .build()
        );

        mockMvc.perform(
                patch("/api/admin/products/" + product.getId() + "/images/" + image.getId() + "/primary")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );

        // Verify primary set
        ProductImage updated = imageRepository.findById(image.getId()).orElseThrow();
        assertTrue(updated.getIsPrimary());
    }

    // ==================== ADMIN: REORDER ====================

    @Test
    @Disabled
    void adminReorderImagesSuccess() throws Exception {
        User admin = TestHelper.createAdminUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(admin.getId(), admin.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductImage image1 = imageRepository.save(
                ProductImage.builder()
                        .product(product)
                        .imageUrl("https://example.com/image1.jpg")
                        .displayOrder(0)
                        .isPrimary(false)
                        .build()
        );
        ProductImage image2 = imageRepository.save(
                ProductImage.builder()
                        .product(product)
                        .imageUrl("https://example.com/image2.jpg")
                        .displayOrder(1)
                        .isPrimary(false)
                        .build()
        );

        // Reorder: image2 first, then image1
        ReorderImagesRequest request = new ReorderImagesRequest();
        request.setImageIds(List.of(image2.getId(), image1.getId()));

        mockMvc.perform(
                patch("/api/admin/products/" + product.getId() + "/images/reorder")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        );
    }
}

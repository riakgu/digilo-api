package com.riakgu.digilo.cart;

import com.riakgu.digilo.TestDataFactory;
import com.riakgu.digilo.TestHelper;
import com.riakgu.digilo.cart.dto.AddToCartRequest;
import com.riakgu.digilo.cart.dto.CartResponse;
import com.riakgu.digilo.cart.dto.UpdateCartItemRequest;
import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.config.TestMockConfig;
import com.riakgu.digilo.product.DeliveryType;
import com.riakgu.digilo.product.Product;
import com.riakgu.digilo.product.ProductRepository;
import com.riakgu.digilo.product.ProductVariant;
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
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

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
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        variantRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== GET CART ====================

    @Test
    void getCartSuccess() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        mockMvc.perform(
                get("/api/user/cart")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<CartResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertNotNull(response.getData());
        });
    }

    @Test
    void getCartUnauthorized() throws Exception {
        mockMvc.perform(
                get("/api/user/cart")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isUnauthorized()
        );
    }

    // ==================== ADD TO CART ====================

    @Test
    void addToCartSuccess() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(TestDataFactory.variantBuilder(product).isActive(true).deliveryType(DeliveryType.MANUAL).build());

        AddToCartRequest request = new AddToCartRequest();
        request.setVariantId(variant.getId());
        request.setQuantity(2);

        mockMvc.perform(
                post("/api/user/cart/items")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<CartResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
            assertFalse(response.getData().getItems().isEmpty());
        });
    }

    @Test
    void addToCartBadRequest() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        AddToCartRequest request = new AddToCartRequest();
        // Missing variantId

        mockMvc.perform(
                post("/api/user/cart/items")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        );
    }

    @Test
    void addToCartVariantNotFound() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        AddToCartRequest request = new AddToCartRequest();
        request.setVariantId(999999L);
        request.setQuantity(1);

        mockMvc.perform(
                post("/api/user/cart/items")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== UPDATE CART ITEM ====================

    @Test
    void updateCartItemSuccess() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(TestDataFactory.variantBuilder(product).isActive(true).deliveryType(DeliveryType.MANUAL).build());

        // Create cart and add item
        Cart cart = cartRepository.save(TestDataFactory.cartBuilder(user).build());
        CartItem cartItem = cartItemRepository.save(TestDataFactory.cartItemBuilder(cart, variant).quantity(1).build());

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(5);

        mockMvc.perform(
                put("/api/user/cart/items/" + cartItem.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            ApiResponse<CartResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertNull(response.getErrors());
        });
    }

    @Test
    void updateCartItemNotFound() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(5);

        mockMvc.perform(
                put("/api/user/cart/items/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== REMOVE FROM CART ====================

    @Test
    void removeFromCartSuccess() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(TestDataFactory.variantBuilder(product).isActive(true).build());

        Cart cart = cartRepository.save(TestDataFactory.cartBuilder(user).build());
        CartItem cartItem = cartItemRepository.save(TestDataFactory.cartItemBuilder(cart, variant).build());

        mockMvc.perform(
                delete("/api/user/cart/items/" + cartItem.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );

        // Verify removed
        assertFalse(cartItemRepository.existsById(cartItem.getId()));
    }

    @Test
    void removeFromCartNotFound() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        mockMvc.perform(
                delete("/api/user/cart/items/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isNotFound()
        );
    }

    // ==================== CLEAR CART ====================

    @Test
    void clearCartSuccess() throws Exception {
        User user = TestHelper.createTestUser(userRepository);
        String authHeader = TestHelper.getAuthHeader(user.getId(), user.getRole());

        Product product = productRepository.save(TestDataFactory.buildProduct());
        ProductVariant variant = variantRepository.save(TestDataFactory.variantBuilder(product).isActive(true).build());

        Cart cart = cartRepository.save(TestDataFactory.cartBuilder(user).build());
        cartItemRepository.save(TestDataFactory.cartItemBuilder(cart, variant).build());

        mockMvc.perform(
                delete("/api/user/cart")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
        ).andExpectAll(
                status().isOk()
        );
    }
}

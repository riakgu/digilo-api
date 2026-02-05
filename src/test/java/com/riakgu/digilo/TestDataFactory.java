package com.riakgu.digilo;

import com.riakgu.digilo.cart.Cart;
import com.riakgu.digilo.cart.CartItem;
import com.riakgu.digilo.category.Category;
import com.riakgu.digilo.notification.Notification;
import com.riakgu.digilo.notification.NotificationType;
import com.riakgu.digilo.order.Order;
import com.riakgu.digilo.order.OrderItem;
import com.riakgu.digilo.order.OrderStatus;
import com.riakgu.digilo.payment.Payment;
import com.riakgu.digilo.payment.PaymentStatus;
import com.riakgu.digilo.product.*;
import com.riakgu.digilo.promo.DiscountType;
import com.riakgu.digilo.promo.Promo;
import com.riakgu.digilo.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Factory class for creating test entities with default values.
 */
public class TestDataFactory {

    private TestDataFactory() {
    }

    // ==================== Category ====================

    public static Category.CategoryBuilder categoryBuilder() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return Category.builder()
                .name("Test Category " + uniqueId)
                .slug("test-category-" + uniqueId)
                .description("Test category description")
                .isActive(true);
    }

    public static Category buildCategory() {
        return categoryBuilder().build();
    }

    // ==================== Product ====================

    public static Product.ProductBuilder productBuilder() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return Product.builder()
                .name("Test Product " + uniqueId)
                .slug("test-product-" + uniqueId)
                .description("Test product description")
                .isActive(true);
    }

    public static Product buildProduct() {
        return productBuilder().build();
    }

    // ==================== ProductVariant ====================

    public static ProductVariant.ProductVariantBuilder variantBuilder(Product product) {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return ProductVariant.builder()
                .product(product)
                .sku("SKU-" + uniqueId)
                .name("Variant " + uniqueId)
                .price(new BigDecimal("99000.00"))
                .deliveryType(DeliveryType.AUTO)
                .durationDays(30)
                .warrantyDays(7)
                .isActive(true);
    }

    public static ProductVariant buildVariant(Product product) {
        return variantBuilder(product).build();
    }

    // ==================== ProductInventory ====================

    public static ProductInventory.ProductInventoryBuilder inventoryBuilder(ProductVariant variant) {
        return ProductInventory.builder()
                .variant(variant)
                .credential("test-credential-data")
                .status(InventoryStatus.AVAILABLE);
    }

    public static ProductInventory buildInventory(ProductVariant variant) {
        return inventoryBuilder(variant).build();
    }

    // ==================== Cart ====================

    public static Cart.CartBuilder cartBuilder(User user) {
        return Cart.builder()
                .user(user);
    }

    public static Cart buildCart(User user) {
        return cartBuilder(user).build();
    }

    // ==================== CartItem ====================

    public static CartItem.CartItemBuilder cartItemBuilder(Cart cart, ProductVariant variant) {
        return CartItem.builder()
                .cart(cart)
                .variant(variant)
                .quantity(1);
    }

    public static CartItem buildCartItem(Cart cart, ProductVariant variant) {
        return cartItemBuilder(cart, variant).build();
    }

    // ==================== Order ====================

    public static Order.OrderBuilder orderBuilder(User user) {
        String orderNumber = "ORD-" + System.currentTimeMillis();
        return Order.builder()
                .user(user)
                .orderNumber(orderNumber)
                .status(OrderStatus.PENDING)
                .subtotal(new BigDecimal("99000.00"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("99000.00"));
    }

    public static Order buildOrder(User user) {
        return orderBuilder(user).build();
    }

    // ==================== OrderItem ====================

    public static OrderItem.OrderItemBuilder orderItemBuilder(Order order, ProductVariant variant) {
        return OrderItem.builder()
                .order(order)
                .variant(variant)
                .productName(variant.getProduct().getName())
                .variantName(variant.getName())
                .quantity(1)
                .price(variant.getPrice());
    }

    public static OrderItem buildOrderItem(Order order, ProductVariant variant) {
        return orderItemBuilder(order, variant).build();
    }

    // ==================== Payment ====================

    public static Payment.PaymentBuilder paymentBuilder(Order order) {
        String providerOrderId = "TXN-" + System.currentTimeMillis();
        return Payment.builder()
                .order(order)
                .provider("MIDTRANS")
                .paymentType("qris")
                .providerOrderId(providerOrderId)
                .amount(order.getTotalAmount())
                .currency("IDR")
                .status(PaymentStatus.PENDING)
                .qrCodeUrl("https://example.com/qr/" + providerOrderId)
                .expiredAt(Instant.now().plusSeconds(900));
    }

    public static Payment buildPayment(Order order) {
        return paymentBuilder(order).build();
    }

    // ==================== Promo ====================

    public static Promo.PromoBuilder promoBuilder() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return Promo.builder()
                .code("PROMO" + uniqueId)
                .description("Test promo code")
                .discountType(DiscountType.PERCENT)
                .discountValue(new BigDecimal("10.00"))
                .minOrderAmount(new BigDecimal("50000.00"))
                .maxDiscount(new BigDecimal("50000.00"))
                .maxUsagePerUser(100)
                .usedCount(0)
                .startsAt(Instant.now().minusSeconds(86400))
                .expiresAt(Instant.now().plusSeconds(86400 * 30))
                .isActive(true);
    }

    public static Promo buildPromo() {
        return promoBuilder().build();
    }

    // ==================== Notification ====================

    public static Notification.NotificationBuilder notificationBuilder(User user) {
        return Notification.builder()
                .user(user)
                .type(NotificationType.ORDER_CREATED)
                .title("Test Notification")
                .message("This is a test notification message")
                .isRead(false);
    }

    public static Notification buildNotification(User user) {
        return notificationBuilder(user).build();
    }
}

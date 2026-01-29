package com.riakgu.digilo.order;

import com.riakgu.digilo.cart.Cart;
import com.riakgu.digilo.cart.CartItem;
import com.riakgu.digilo.cart.CartRepository;
import com.riakgu.digilo.common.exception.BadRequestException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.order.dto.*;
import com.riakgu.digilo.product.*;
import com.riakgu.digilo.promo.Promo;
import com.riakgu.digilo.promo.PromoService;
import com.riakgu.digilo.user.User;
import com.riakgu.digilo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final ProductInventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final PromoService promoService;
    private final ProductImageHelper productImageHelper;

    @Transactional
    public OrderResponse createFromCart(Long userId, CreateOrderRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Validate stock for AUTO delivery BEFORE creating order
        for (CartItem cartItem : cart.getItems()) {
            if (cartItem.getVariant().getDeliveryType() == DeliveryType.AUTO) {
                long availableStock = inventoryRepository.countByVariantIdAndStatus(
                        cartItem.getVariant().getId(), InventoryStatus.AVAILABLE);
                if (availableStock < cartItem.getQuantity()) {
                    throw new BadRequestException("Not enough stock for " + cartItem.getVariant().getName());
                }
            }
        }

        // Calculate subtotal
        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getVariant().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Apply promo
        Promo promo = null;
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (request != null && request.getPromoCode() != null) {
            promo = promoService.getValidPromo(request.getPromoCode(), userId, subtotal);
            discountAmount = promoService.calculateDiscount(promo, subtotal);
        }

        BigDecimal totalAmount = subtotal.subtract(discountAmount);

        // Create order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .promo(promo)
                .notes(request != null ? request.getNotes() : null)
                .build();

        orderRepository.save(order);

        for (CartItem cartItem : cart.getItems()) {
            ProductVariant variant = cartItem.getVariant();
            Product product = variant.getProduct();
            
            String productImageUrl = productImageHelper.getDisplayImageUrl(product);
            
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .variant(variant)
                    .variantName(variant.getName())
                    .productName(product.getName())
                    .productImageUrl(productImageUrl)
                    .price(variant.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build();

            orderItemRepository.save(orderItem);
            order.getItems().add(orderItem);
        }

        for (OrderItem orderItem : order.getItems()) {
            DeliveryType deliveryType = orderItem.getVariant().getDeliveryType();
            Long variantId = orderItem.getVariant().getId();
            int quantity = orderItem.getQuantity();

            if (deliveryType == DeliveryType.AUTO) {
                reserveInventory(orderItem.getId(), variantId, quantity);

            } else if (deliveryType == DeliveryType.HYBRID) {
                long availableStock = inventoryRepository.countByVariantIdAndStatus(
                        variantId, InventoryStatus.AVAILABLE);

                int toReserve = (int) Math.min(availableStock, quantity);

                if (toReserve > 0) {
                    reserveInventory(orderItem.getId(), variantId, toReserve);
                }
            }
        }

        cart.getItems().clear();
        cartRepository.save(cart);

        // Record promo usage after order created
        if (promo != null) {
            promoService.recordUsage(promo, userId, order.getId());
        }

        return OrderResponse.fromEntity(order);
    }


    @Transactional(readOnly = true)
    public OrderResponse getById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Order does not belong to you");
        }

        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getByOrderNumber(String orderNumber, Long userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Order does not belong to you");
        }

        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(OrderResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public OrderResponse getByIdAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(OrderResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable)
                .map(OrderResponse::fromEntity);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();

        order.setStatus(newStatus);

        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }

        orderRepository.save(order);

        if (newStatus == OrderStatus.PAID) {
            markInventoryAsSold(order);
        } else if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.FAILED) {
            releaseInventory(order);
        }

        log.info("Order {} status changed from {} to {}", order.getOrderNumber(), oldStatus, newStatus);

        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public List<OrderCredentialResponse> getOrderCredentials(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Order does not belong to you");
        }

        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.COMPLETED) {
            throw new BadRequestException("Credentials are only available for paid orders");
        }

        return order.getItems().stream()
                .map(this::buildCredentialResponse)
                .filter(response -> !response.getCredentials().isEmpty())
                .collect(Collectors.toList());
    }

    private OrderCredentialResponse buildCredentialResponse(OrderItem item) {
        List<ProductInventory> soldInventories = inventoryRepository
                .findByOrderItemIdAndStatus(item.getId(), InventoryStatus.SOLD);

        List<OrderCredentialResponse.CredentialItem> credentials = soldInventories.stream()
                .map(inv -> {
                    Map<String, Object> decrypted = encryptionService.decrypt(inv.getCredential());
                    return OrderCredentialResponse.CredentialItem.builder()
                            .inventoryId(inv.getId())
                            .credential(decrypted)
                            .build();
                })
                .collect(Collectors.toList());

        return OrderCredentialResponse.builder()
                .orderItemId(item.getId())
                .variantName(item.getVariantName())
                .quantity(item.getQuantity())
                .credentials(credentials)
                .build();
    }

    private void reserveInventory(Long orderItemId, Long variantId, int quantity) {
        List<ProductInventory> available = inventoryRepository
                .findAvailableForUpdate(variantId, PageRequest.of(0, quantity));

        if (available.size() < quantity) {
            throw new BadRequestException("Out of Stock");
        }

        Instant now = Instant.now();
        for (ProductInventory inventory : available) {
            inventory.setStatus(InventoryStatus.RESERVED);
            inventory.setOrderItemId(orderItemId);
            inventory.setReservedAt(now);
        }

        inventoryRepository.saveAll(available);
    }

    private void markInventoryAsSold(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<ProductInventory> toUpdate = new ArrayList<>();
        Instant now = Instant.now();

        for (OrderItem item : items) {
            List<ProductInventory> reserved = inventoryRepository
                    .findByOrderItemIdAndStatus(item.getId(), InventoryStatus.RESERVED);

            for (ProductInventory inv : reserved) {
                inv.setStatus(InventoryStatus.SOLD);
                inv.setSoldAt(now);
                toUpdate.add(inv);
            }
        }

        inventoryRepository.saveAll(toUpdate);
    }


    private void releaseInventory(Order order) {
        List<ProductInventory> toUpdate = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            List<ProductInventory> reserved = inventoryRepository
                    .findByOrderItemIdAndStatus(item.getId(), InventoryStatus.RESERVED);

            for (ProductInventory inv : reserved) {
                inv.setStatus(InventoryStatus.AVAILABLE);
                inv.setReservedAt(null);
                inv.setOrderItemId(null);
                toUpdate.add(inv);
            }
        }

        inventoryRepository.saveAll(toUpdate);
    }

    private String generateOrderNumber() {
        LocalDate today = LocalDate.now();
        String dateStr = String.format("%02d%02d%02d",
                today.getYear() % 100,
                today.getMonthValue(),
                today.getDayOfMonth());

        String randomStr = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();

        String orderNumber = "DIGILO-" + dateStr + "-" + randomStr;

        while (orderRepository.existsByOrderNumber(orderNumber)) {
            randomStr = UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 6)
                    .toUpperCase();
            orderNumber = "DIGILO-" + dateStr + "-" + randomStr;
        }

        return orderNumber;
    }

}
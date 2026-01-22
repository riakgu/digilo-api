package com.riakgu.digilo.order;

import com.riakgu.digilo.cart.Cart;
import com.riakgu.digilo.cart.CartItem;
import com.riakgu.digilo.cart.CartRepository;
import com.riakgu.digilo.common.exception.BadRequestException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.order.dto.*;
import com.riakgu.digilo.product.DeliveryType;
import com.riakgu.digilo.product.InventoryStatus;
import com.riakgu.digilo.product.ProductInventory;
import com.riakgu.digilo.product.ProductInventoryRepository;
import com.riakgu.digilo.user.User;
import com.riakgu.digilo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final ProductInventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createFromCart(Long userId, CreateOrderRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // Validate stock for AUTO delivery variants (HYBRID can proceed with partial/no stock)
        for (CartItem cartItem : cart.getItems()) {
            DeliveryType deliveryType = cartItem.getVariant().getDeliveryType();

            if (deliveryType == DeliveryType.AUTO) {
                long availableStock = inventoryRepository.countByVariantIdAndStatus(
                        cartItem.getVariant().getId(), InventoryStatus.AVAILABLE);
                if (availableStock < cartItem.getQuantity()) {
                    throw new BadRequestException("Not enough stock for " + cartItem.getVariant().getName());
                }
            }
            // MANUAL: No validation needed - always allow
            // HYBRID: No validation needed - will auto-deliver if stock available, manual if not
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Calculate total
        BigDecimal totalAmount = cart.getItems().stream()
                .map(item -> item.getVariant().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .notes(request != null ? request.getNotes() : null)
                .build();

        orderRepository.save(order);

        // Create order items with price snapshot
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .variant(cartItem.getVariant())
                    .variantName(cartItem.getVariant().getName())
                    .price(cartItem.getVariant().getPrice())
                    .quantity(cartItem.getQuantity())
                    .build();

            orderItemRepository.save(orderItem);
            order.getItems().add(orderItem);
        }

        // Reserve inventory based on delivery type
        for (CartItem cartItem : cart.getItems()) {
            DeliveryType deliveryType = cartItem.getVariant().getDeliveryType();
            Long variantId = cartItem.getVariant().getId();
            int quantity = cartItem.getQuantity();

            if (deliveryType == DeliveryType.AUTO) {
                // AUTO: Must reserve all
                reserveInventory(variantId, quantity);
            } else if (deliveryType == DeliveryType.HYBRID) {
                // HYBRID: Reserve as many as available
                long availableStock = inventoryRepository.countByVariantIdAndStatus(
                        variantId, InventoryStatus.AVAILABLE);
                int toReserve = (int) Math.min(availableStock, quantity);
                if (toReserve > 0) {
                    reserveInventory(variantId, toReserve);
                }
                // Rest will be fulfilled manually by admin
            }
            // MANUAL: No reservation - admin will provide manually
        }

        // Clear cart after order created
        cart.getItems().clear();
        cartRepository.save(cart);

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

    private void reserveInventory(Long variantId, int quantity) {
        for (int i = 0; i < quantity; i++) {
            ProductInventory inventory = inventoryRepository
                    .findFirstByVariantIdAndStatus(variantId, InventoryStatus.AVAILABLE)
                    .orElseThrow(() -> new BadRequestException("No available inventory"));

            inventory.setStatus(InventoryStatus.RESERVED);
            inventory.setReservedAt(Instant.now());
            inventoryRepository.save(inventory);
        }
    }

    private void markInventoryAsSold(Order order) {
        for (OrderItem item : order.getItems()) {
            DeliveryType deliveryType = item.getVariant().getDeliveryType();

            if (deliveryType == DeliveryType.AUTO || deliveryType == DeliveryType.HYBRID) {
                // Mark reserved inventory as sold
                List<ProductInventory> reserved = inventoryRepository
                        .findByVariantIdAndStatus(item.getVariant().getId(), InventoryStatus.RESERVED);

                int count = 0;
                for (ProductInventory inv : reserved) {
                    if (count >= item.getQuantity()) break;
                    inv.setStatus(InventoryStatus.SOLD);
                    inv.setSoldAt(Instant.now());
                    inv.setOrderItemId(item.getId());
                    inventoryRepository.save(inv);
                    count++;
                }
            }
            // MANUAL: No inventory to mark - admin provides manually
        }
    }

    private void releaseInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getVariant().getDeliveryType() == DeliveryType.AUTO) {
                List<ProductInventory> reserved = inventoryRepository
                        .findByVariantIdAndStatus(item.getVariant().getId(), InventoryStatus.RESERVED);

                int count = 0;
                for (ProductInventory inv : reserved) {
                    if (count >= item.getQuantity()) break;
                    inv.setStatus(InventoryStatus.AVAILABLE);
                    inv.setReservedAt(null);
                    inventoryRepository.save(inv);
                    count++;
                }
            }
        }
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
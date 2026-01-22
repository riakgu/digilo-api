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
import org.springframework.data.domain.PageRequest;
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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        BigDecimal totalAmount = cart.getItems().stream()
                .map(item -> item.getVariant().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .notes(request != null ? request.getNotes() : null)
                .build();

        orderRepository.save(order);

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

    private void reserveInventory(Long orderItemId, Long variantId, int quantity) {
        for (int i = 0; i < quantity; i++) {

            ProductInventory inventory = inventoryRepository
                    .findAvailableForUpdate(
                            variantId,
                            PageRequest.of(0, 1)
                    )
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Out of Stock"));

            inventory.setStatus(InventoryStatus.RESERVED);
            inventory.setOrderItemId(orderItemId);
            inventory.setReservedAt(Instant.now());

            inventoryRepository.save(inventory);
        }
    }

    private void markInventoryAsSold(Order order) {

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        for (OrderItem item : items) {

            List<ProductInventory> reserved = inventoryRepository
                    .findByOrderItemIdAndStatus(item.getId(), InventoryStatus.RESERVED);

            for (ProductInventory inv : reserved) {
                inv.setStatus(InventoryStatus.SOLD);
                inv.setSoldAt(Instant.now());
                inventoryRepository.save(inv);
            }
        }
    }


    private void releaseInventory(Order order) {
        for (OrderItem item : order.getItems()) {

            List<ProductInventory> reserved = inventoryRepository
                    .findByOrderItemIdAndStatus(item.getId(), InventoryStatus.RESERVED);

            for (ProductInventory inv : reserved) {
                inv.setStatus(InventoryStatus.AVAILABLE);
                inv.setReservedAt(null);
                inv.setOrderItemId(null);
                inventoryRepository.save(inv);
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
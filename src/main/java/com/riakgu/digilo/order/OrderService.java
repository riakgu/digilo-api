package com.riakgu.digilo.order;

import com.riakgu.digilo.cart.Cart;
import com.riakgu.digilo.cart.CartItem;
import com.riakgu.digilo.cart.CartRepository;
import com.riakgu.digilo.common.exception.BadRequestException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.common.service.EncryptionService;
import com.riakgu.digilo.event.EventPublisher;
import com.riakgu.digilo.event.OrderEvent;
import com.riakgu.digilo.order.dto.*;
import com.riakgu.digilo.product.*;
import com.riakgu.digilo.product.image.ProductImageHelper;
import com.riakgu.digilo.product.inventory.InventoryStatus;
import com.riakgu.digilo.product.inventory.ProductInventory;
import com.riakgu.digilo.product.inventory.ProductInventoryRepository;
import com.riakgu.digilo.product.variant.DeliveryType;
import com.riakgu.digilo.product.variant.ProductVariant;
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
import java.util.Collection;
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
    private final EventPublisher eventPublisher;

    @Transactional
    public OrderResponse createFromCart(Long userId, CreateOrderRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        validateAutoDeliveryStock(cart.getItems());

        BigDecimal subtotal = calculateSubtotal(cart.getItems());

        Promo promo = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request != null && request.getPromoCode() != null) {
            promo = promoService.getValidPromo(request.getPromoCode(), userId, subtotal);
            discountAmount = promoService.calculateDiscount(promo, subtotal);
        }

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .totalAmount(subtotal.subtract(discountAmount))
                .promo(promo)
                .notes(request != null ? request.getNotes() : null)
                .build();

        orderRepository.save(order);
        log.info("Order created: orderNumber={}, userId={}, itemCount={}, totalAmount={}",
                order.getOrderNumber(), userId, cart.getItems().size(), order.getTotalAmount());

        createOrderItems(order, cart.getItems());
        reserveOrderInventory(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        if (promo != null) {
            promoService.recordUsage(promo, userId, order.getId());
        }

        log.info("Order {} completed processing: status={}, promoApplied={}",
                order.getOrderNumber(), order.getStatus(), promo != null);

        eventPublisher.publishOrderEvent(
                OrderEvent.orderCreated(order.getOrderNumber(), userId, order.getTotalAmount()));

        return OrderResponse.fromEntity(order);
    }

    private void validateAutoDeliveryStock(Collection<CartItem> items) {
        List<Long> autoVariantIds = items.stream()
                .filter(item -> item.getVariant().getDeliveryType() == DeliveryType.AUTO)
                .map(item -> item.getVariant().getId())
                .collect(Collectors.toList());

        if (autoVariantIds.isEmpty()) return;

        Map<Long, Long> stockMap = inventoryRepository
                .countByVariantIdsAndStatus(autoVariantIds, InventoryStatus.AVAILABLE)
                .stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        for (CartItem item : items) {
            if (item.getVariant().getDeliveryType() == DeliveryType.AUTO) {
                long available = stockMap.getOrDefault(item.getVariant().getId(), 0L);
                if (available < item.getQuantity()) {
                    throw new BadRequestException("Not enough stock for " + item.getVariant().getName());
                }
            }
        }
    }

    private BigDecimal calculateSubtotal(Collection<CartItem> items) {
        return items.stream()
                .map(item -> item.getVariant().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void createOrderItems(Order order, Collection<CartItem> cartItems) {
        for (CartItem cartItem : cartItems) {
            ProductVariant variant = cartItem.getVariant();
            Product product = variant.getProduct();

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .variant(variant)
                    .variantName(variant.getName())
                    .productName(product.getName())
                    .productImageUrl(productImageHelper.getDisplayImageUrl(product))
                    .price(variant.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build();

            orderItemRepository.save(orderItem);
            order.getItems().add(orderItem);
        }
    }

    private void reserveOrderInventory(Order order) {
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

    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Order does not belong to you");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Only pending orders can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        releaseInventory(order);

        eventPublisher.publishOrderEvent(
                OrderEvent.orderCancelled(order.getOrderNumber(), userId, order.getTotalAmount()));

        log.info("Order {} cancelled by user {}", order.getOrderNumber(), userId);

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
    public Page<OrderResponse> getMyOrders(Long userId, String orderNumber, Pageable pageable) {
        if (orderNumber != null && !orderNumber.isBlank()) {
            // Return single order if found
            Order order = orderRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new NotFoundException("Order not found"));
            if (!order.getUser().getId().equals(userId)) {
                throw new BadRequestException("Order does not belong to you");
            }
            return new org.springframework.data.domain.PageImpl<>(
                    List.of(OrderResponse.fromEntity(order)), pageable, 1);
        }
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
    public Page<OrderResponse> getAllOrders(String orderNumber, Long userId, OrderStatus status, Pageable pageable) {
        return orderRepository.findAllWithFilters(orderNumber, userId, status, pageable)
                .map(OrderResponse::fromEntity);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();

        // Validate status transition
        if (!isValidStatusTransition(oldStatus, newStatus)) {
            throw new BadRequestException("Invalid status transition: " + oldStatus + " → " + newStatus);
        }

        order.setStatus(newStatus);

        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }

        orderRepository.save(order);

        if (newStatus == OrderStatus.PAID) {
            markInventoryAsSold(order);
            eventPublisher.publishOrderEvent(
                    OrderEvent.orderPaid(order.getOrderNumber(), order.getUser().getId(), order.getTotalAmount()));
            
            // Auto-complete if all items are fully delivered (for AUTO delivery)
            if (isOrderFullyDelivered(order)) {
                order.setStatus(OrderStatus.COMPLETED);
                orderRepository.save(order);
                eventPublisher.publishOrderEvent(
                        OrderEvent.orderCompleted(order.getOrderNumber(), order.getUser().getId(), order.getTotalAmount()));
                log.info("Order {} auto-completed: all credentials delivered", order.getOrderNumber());
            }
        } else if (newStatus == OrderStatus.CANCELLED) {
            releaseInventory(order);
            eventPublisher.publishOrderEvent(
                    OrderEvent.orderCancelled(order.getOrderNumber(), order.getUser().getId(), order.getTotalAmount()));
        } else if (newStatus == OrderStatus.FAILED) {
            releaseInventory(order);
            eventPublisher.publishOrderEvent(
                    OrderEvent.orderFailed(order.getOrderNumber(), order.getUser().getId(), order.getTotalAmount()));
        }

        log.info("Order {} status changed from {} to {}", order.getOrderNumber(), oldStatus, newStatus);

        return OrderResponse.fromEntity(order);
    }

    @Transactional
    public OrderCredentialResponse assignCredential(Long orderId, Long orderItemId, Long inventoryId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new BadRequestException("Can only assign credentials to PAID orders");
        }

        OrderItem orderItem = order.getItems().stream()
                .filter(item -> item.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Order item not found"));

        ProductInventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventory not found"));

        // Validate variant match
        if (!inventory.getVariant().getId().equals(orderItem.getVariant().getId())) {
            throw new BadRequestException("Inventory does not match order item variant");
        }

        // Validate inventory is available
        if (inventory.getStatus() != InventoryStatus.AVAILABLE) {
            throw new BadRequestException("Inventory is not available (status: " + inventory.getStatus() + ")");
        }

        // Mark inventory as SOLD and assign to order item
        inventory.setStatus(InventoryStatus.SOLD);
        inventory.setOrderItemId(orderItem.getId());
        inventory.setSoldAt(Instant.now());
        inventoryRepository.save(inventory);

        log.info("Credential manually assigned: orderId={}, orderItemId={}, inventoryId={}", 
                orderId, orderItemId, inventoryId);

        // Check if all order items are fully delivered, then auto-complete
        if (isOrderFullyDelivered(order)) {
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
            eventPublisher.publishOrderEvent(
                    OrderEvent.orderCompleted(order.getOrderNumber(), order.getUser().getId(), order.getTotalAmount()));
            log.info("Order {} auto-completed: all credentials delivered", order.getOrderNumber());
        }

        return buildCredentialResponse(orderItem);
    }

    private boolean isOrderFullyDelivered(Order order) {
        for (OrderItem item : order.getItems()) {
            long deliveredCount = inventoryRepository.countByOrderItemIdAndStatus(
                    item.getId(), InventoryStatus.SOLD);
            if (deliveredCount < item.getQuantity()) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidStatusTransition(OrderStatus from, OrderStatus to) {
        if (from == to) {
            return false; // No change
        }
        return switch (from) {
            case PENDING -> to == OrderStatus.PAID || to == OrderStatus.CANCELLED || to == OrderStatus.FAILED;
            case PAID -> to == OrderStatus.PROCESSING || to == OrderStatus.COMPLETED || 
                         to == OrderStatus.CANCELLED || to == OrderStatus.FAILED || to == OrderStatus.REFUNDED;
            case PROCESSING -> to == OrderStatus.COMPLETED || to == OrderStatus.CANCELLED || to == OrderStatus.FAILED;
            case COMPLETED -> to == OrderStatus.REFUNDED; // Can refund completed orders
            case CANCELLED, FAILED, REFUNDED -> false; // Terminal states
        };
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

    @Transactional(readOnly = true)
    public List<OrderCredentialResponse> getOrderCredentialsAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        return order.getItems().stream()
                .map(this::buildCredentialResponse)
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
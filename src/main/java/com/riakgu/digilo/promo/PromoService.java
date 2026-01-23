package com.riakgu.digilo.promo;

import com.riakgu.digilo.cart.Cart;
import com.riakgu.digilo.cart.CartRepository;
import com.riakgu.digilo.common.exception.BadRequestException;
import com.riakgu.digilo.common.exception.DuplicateResourceException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.order.Order;
import com.riakgu.digilo.order.OrderRepository;
import com.riakgu.digilo.promo.dto.*;
import com.riakgu.digilo.user.User;
import com.riakgu.digilo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromoService {

    private final PromoRepository promoRepository;
    private final PromoUsageRepository promoUsageRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public PromoResponse create(PromoRequest request) {
        String code = request.getCode().toUpperCase().trim();

        if (promoRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Promo code already exists: " + code);
        }

        Promo promo = Promo.builder()
                .code(code)
                .name(request.getName())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxDiscount(request.getMaxDiscount())
                .minOrderAmount(request.getMinOrderAmount())
                .maxTotalUsage(request.getMaxTotalUsage())
                .maxUsagePerUser(request.getMaxUsagePerUser())
                .startsAt(request.getStartsAt())
                .expiresAt(request.getExpiresAt())
                .isActive(request.getIsActive())
                .build();

        promoRepository.save(promo);
        log.info("Promo created: {}", code);

        return PromoResponse.fromEntity(promo);
    }

    @Transactional
    public PromoResponse update(Long id, PromoRequest request) {
        Promo promo = promoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promo not found"));

        String newCode = request.getCode().toUpperCase().trim();
        if (!promo.getCode().equals(newCode) && promoRepository.existsByCode(newCode)) {
            throw new DuplicateResourceException("Promo code already exists: " + newCode);
        }

        promo.setCode(newCode);
        promo.setName(request.getName());
        promo.setDescription(request.getDescription());
        promo.setDiscountType(request.getDiscountType());
        promo.setDiscountValue(request.getDiscountValue());
        promo.setMaxDiscount(request.getMaxDiscount());
        promo.setMinOrderAmount(request.getMinOrderAmount());
        promo.setMaxTotalUsage(request.getMaxTotalUsage());
        promo.setMaxUsagePerUser(request.getMaxUsagePerUser());
        promo.setStartsAt(request.getStartsAt());
        promo.setExpiresAt(request.getExpiresAt());
        promo.setIsActive(request.getIsActive());

        promoRepository.save(promo);

        return PromoResponse.fromEntity(promo);
    }

    @Transactional(readOnly = true)
    public PromoResponse getById(Long id) {
        Promo promo = promoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promo not found"));
        return PromoResponse.fromEntity(promo);
    }

    @Transactional(readOnly = true)
    public Page<PromoResponse> getAll(Pageable pageable) {
        return promoRepository.findAll(pageable)
                .map(PromoResponse::fromEntity);
    }

    @Transactional
    public void activate(Long id) {
        Promo promo = promoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promo not found"));
        promo.setIsActive(true);
        promoRepository.save(promo);
    }

    @Transactional
    public void deactivate(Long id) {
        Promo promo = promoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promo not found"));
        promo.setIsActive(false);
        promoRepository.save(promo);
    }

    @Transactional(readOnly = true)
    public PromoValidationResponse validatePromo(Long userId, String code) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Cart not found"));

        BigDecimal cartTotal = calculateCartTotal(cart);

        try {
            Promo promo = getValidPromo(code, userId, cartTotal);
            BigDecimal discountAmount = calculateDiscount(promo, cartTotal);
            BigDecimal finalTotal = cartTotal.subtract(discountAmount);

            return PromoValidationResponse.builder()
                    .valid(true)
                    .code(promo.getCode())
                    .name(promo.getName())
                    .discountAmount(discountAmount)
                    .originalTotal(cartTotal)
                    .finalTotal(finalTotal)
                    .message("Promo valid!")
                    .build();

        } catch (BadRequestException e) {
            return PromoValidationResponse.builder()
                    .valid(false)
                    .code(code)
                    .originalTotal(cartTotal)
                    .finalTotal(cartTotal)
                    .message(e.getMessage())
                    .build();
        }
    }

    public Promo getValidPromo(String code, Long userId, BigDecimal orderTotal) {
        Promo promo = promoRepository.findByCodeAndIsActive(code.toUpperCase(), true)
                .orElseThrow(() -> new BadRequestException("Promo code not found or inactive"));

        Instant now = Instant.now();

        // Check date validity
        if (promo.getStartsAt() != null && now.isBefore(promo.getStartsAt())) {
            throw new BadRequestException("Promo has not started yet");
        }
        if (promo.getExpiresAt() != null && now.isAfter(promo.getExpiresAt())) {
            throw new BadRequestException("Promo has expired");
        }

        // Check minimum order amount
        if (promo.getMinOrderAmount() != null && orderTotal.compareTo(promo.getMinOrderAmount()) < 0) {
            throw new BadRequestException("Minimum order amount is " + promo.getMinOrderAmount());
        }

        // Check total usage limit
        if (promo.getMaxTotalUsage() != null && promo.getUsedCount() >= promo.getMaxTotalUsage()) {
            throw new BadRequestException("Promo usage limit reached");
        }

        // Check per-user usage limit
        if (promo.getMaxUsagePerUser() != null) {
            long userUsageCount = promoUsageRepository.countByPromoIdAndUserId(promo.getId(), userId);
            if (userUsageCount >= promo.getMaxUsagePerUser()) {
                throw new BadRequestException("You have already used this promo");
            }
        }

        return promo;
    }

    public BigDecimal calculateDiscount(Promo promo, BigDecimal orderTotal) {
        BigDecimal discount;

        if (promo.getDiscountType() == DiscountType.PERCENT) {
            discount = orderTotal.multiply(promo.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Apply max discount cap
            if (promo.getMaxDiscount() != null && discount.compareTo(promo.getMaxDiscount()) > 0) {
                discount = promo.getMaxDiscount();
            }
        } else {
            // FIXED discount
            discount = promo.getDiscountValue();
        }

        // Don't exceed order total
        if (discount.compareTo(orderTotal) > 0) {
            discount = orderTotal;
        }

        return discount;
    }

    @Transactional
    public void recordUsage(Promo promo, Long userId, Long orderId) {
        User user = userRepository.getReferenceById(userId);
        Order order = orderRepository.getReferenceById(orderId);

        PromoUsage usage = PromoUsage.builder()
                .promo(promo)
                .user(user)
                .order(order)
                .usedAt(Instant.now())
                .build();

        promoUsageRepository.save(usage);

        // Increment used count
        promo.setUsedCount(promo.getUsedCount() + 1);
        promoRepository.save(promo);

        log.info("Promo {} used by user {} on order {}", promo.getCode(), userId, orderId);
    }

    private BigDecimal calculateCartTotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getVariant().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
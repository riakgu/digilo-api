package com.riakgu.digilo.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserId(Long userId);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    boolean existsByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o WHERE " +
            "(:orderNumber IS NULL OR o.orderNumber LIKE %:orderNumber%) " +
            "AND (:userId IS NULL OR o.user.id = :userId) " +
            "AND (:status IS NULL OR o.status = :status)")
    Page<Order> findAllWithFilters(
            @Param("orderNumber") String orderNumber,
            @Param("userId") Long userId,
            @Param("status") OrderStatus status,
            Pageable pageable
    );
}
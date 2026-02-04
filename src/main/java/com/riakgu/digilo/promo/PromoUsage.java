package com.riakgu.digilo.promo;

import com.riakgu.digilo.order.Order;
import com.riakgu.digilo.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "promo_usages", 
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"order_id"})
    },
    indexes = {
        @Index(name = "idx_promo_usage_promo_id", columnList = "promo_id"),
        @Index(name = "idx_promo_usage_user_id", columnList = "user_id"),
        @Index(name = "idx_promo_usage_promo_user", columnList = "promo_id, user_id")
    }
)
public class PromoUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_id", nullable = false)
    private Promo promo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "used_at")
    private Instant usedAt = Instant.now();
}
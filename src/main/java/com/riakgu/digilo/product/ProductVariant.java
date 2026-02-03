package com.riakgu.digilo.product;

import com.riakgu.digilo.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_variants", indexes = {
    @Index(name = "idx_variant_product_id", columnList = "product_id"),
    @Index(name = "idx_variant_sku", columnList = "sku")
})
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false)
    private DeliveryType deliveryType = DeliveryType.AUTO;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "warranty_days")
    private Integer warrantyDays;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

}

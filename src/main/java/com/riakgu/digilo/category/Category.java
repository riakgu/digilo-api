package com.riakgu.digilo.category;

import com.riakgu.digilo.common.base.BaseEntity;
import com.riakgu.digilo.product.ProductCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categories", indexes = {
        @Index(name = "idx_category_slug", columnList = "slug"),
        @Index(name = "idx_category_is_active", columnList = "is_active")
})
public class Category extends BaseEntity {

    @OneToMany(mappedBy = "category")
    private Set<ProductCategory> products = new HashSet<>();

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}

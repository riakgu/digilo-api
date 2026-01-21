package com.riakgu.digilo.product;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProductCategoryId implements Serializable {
    private Long productId;
    private Long categoryId;
}
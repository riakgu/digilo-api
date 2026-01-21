package com.riakgu.digilo.product.dto;

import com.riakgu.digilo.product.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageResponse {

    private Long id;
    private String imageUrl;
    private Boolean isPrimary;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProductImageResponse fromEntity(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .isPrimary(image.getIsPrimary())
                .createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt())
                .build();
    }
}

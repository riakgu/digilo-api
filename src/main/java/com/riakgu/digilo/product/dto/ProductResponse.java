package com.riakgu.digilo.product.dto;

import com.riakgu.digilo.category.dto.CategoryResponse;
import com.riakgu.digilo.product.Product;
import com.riakgu.digilo.product.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private List<String> categories;
    private List<String> images;

    public static ProductResponse fromEntity(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .imageUrl(product.getImages().stream()
                        .filter(ProductImage::getIsPrimary)
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        .orElse(null))
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .categories(product.getCategories().stream()
                        .map(pc -> pc.getCategory().getSlug())
                        .collect(Collectors.toList()))
                .images(product.getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .collect(Collectors.toList()))
                .build();
    }
}
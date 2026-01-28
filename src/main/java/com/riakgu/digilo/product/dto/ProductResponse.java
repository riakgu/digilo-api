package com.riakgu.digilo.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.riakgu.digilo.category.dto.CategoryResponse;
import com.riakgu.digilo.product.Product;
import com.riakgu.digilo.product.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private List<String> categories;
    private List<String> images;
    private List<ProductVariantResponse> variants;

    public static ProductResponse fromEntity(Product product, List<ProductVariantResponse> variants) {
        return fromEntity(product, variants, null);
    }

    public static ProductResponse fromEntity(Product product, List<ProductVariantResponse> variants, String imageUrl) {
        BigDecimal minPrice = variants.stream()
                .map(ProductVariantResponse::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(null);
        BigDecimal maxPrice = variants.stream()
                .map(ProductVariantResponse::getPrice)
                .max(BigDecimal::compareTo)
                .orElse(null);
        
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .imageUrl(imageUrl)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .categories(product.getCategories().stream()
                        .map(pc -> pc.getCategory().getName())
                        .collect(Collectors.toList()))
                .images(product.getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .collect(Collectors.toList()))
                .variants(variants)
                .build();
    }
}
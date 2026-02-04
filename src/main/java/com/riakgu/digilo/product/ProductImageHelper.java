package com.riakgu.digilo.product;

import com.riakgu.digilo.config.ImagesProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class ProductImageHelper {

    private final ImagesProperties imagesProperties;

    public String getDisplayImageUrl(Product product) {
        if (product == null || product.getImages() == null || product.getImages().isEmpty()) {
            return imagesProperties.getProductPlaceholder();
        }

        // Try primary image first
        return product.getImages().stream()
                .filter(ProductImage::getIsPrimary)
                .map(ProductImage::getImageUrl)
                .findFirst()
                // Fallback to first by displayOrder
                .orElseGet(() -> product.getImages().stream()
                        .sorted(Comparator.comparing(img -> img.getDisplayOrder() != null ? img.getDisplayOrder() : 0))
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        // Final fallback to default
                        .orElse(imagesProperties.getProductPlaceholder()));
    }
}

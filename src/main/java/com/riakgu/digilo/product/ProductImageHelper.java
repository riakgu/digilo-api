package com.riakgu.digilo.product;

import com.riakgu.digilo.config.ImagesProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
                // Fallback to any image
                .orElseGet(() -> product.getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        // Final fallback to default
                        .orElse(imagesProperties.getProductPlaceholder()));
    }
}

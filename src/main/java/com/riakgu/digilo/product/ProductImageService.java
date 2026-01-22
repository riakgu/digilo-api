package com.riakgu.digilo.product;

import com.riakgu.digilo.common.exception.BadRequestException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.product.dto.ProductImageRequest;
import com.riakgu.digilo.product.dto.ProductImageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final R2ImageService r2ImageService;
    private final ProductRepository productRepository;

    @Transactional
    public void addImage(Long productId, ProductImageRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found"));

        boolean shouldBePrimary = product.getImages().isEmpty() || request.getIsPrimary();

        if (shouldBePrimary) {
            product.getImages().forEach(img -> img.setIsPrimary(false));
        }

        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(request.getImageUrl())
                .isPrimary(shouldBePrimary)
                .build();

        product.getImages().add(image);
        productRepository.save(product);
    }

    @Transactional
    public void setPrimaryImage(Long productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found"));

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Image with id " + imageId + " not found"));

        if (!image.getProduct().getId().equals(productId)) {
            throw new BadRequestException("Image does not belong to this product");
        }

        product.getImages().forEach(img -> img.setIsPrimary(false));

        image.setIsPrimary(true);
        productImageRepository.save(image);
    }

    @Transactional
    public void deleteImage(Long productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found"));

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Image with id " + imageId + " not found"));

        if (!image.getProduct().getId().equals(productId)) {
            throw new BadRequestException("Image does not belong to this product");
        }


        boolean wasPrimary = image.getIsPrimary();

        String imageUrl = image.getImageUrl();
        String fileName = r2ImageService.extractFileNameIfR2(imageUrl);
        if (fileName != null) {
            r2ImageService.deleteImage(fileName);
        }

        product.getImages().remove(image);
        productImageRepository.delete(image);

        if (wasPrimary && !product.getImages().isEmpty()) {
            product.getImages().iterator().next().setIsPrimary(true);
            productRepository.save(product);
        }
    }

    @Transactional(readOnly = true)
    public List<ProductImageResponse> getImagesByProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found"));

        return product.getImages().stream()
                .map(ProductImageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateImage(Long productId, Long imageId, ProductImageRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found"));

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Image with id " + imageId + " not found"));

        if (!image.getProduct().getId().equals(productId)) {
            throw new BadRequestException("Image does not belong to this product");
        }

        image.setImageUrl(request.getImageUrl());

        if (request.getIsPrimary() && !image.getIsPrimary()) {
            product.getImages().forEach(img -> img.setIsPrimary(false));
            image.setIsPrimary(true);
        }

        productImageRepository.save(image);
    }
}

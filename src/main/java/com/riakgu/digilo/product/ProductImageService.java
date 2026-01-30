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
import java.util.Map;
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

        int maxOrder = product.getImages().stream()
                .mapToInt(img -> img.getDisplayOrder() != null ? img.getDisplayOrder() : 0)
                .max()
                .orElse(-1);

        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(request.getImageUrl())
                .isPrimary(shouldBePrimary)
                .displayOrder(maxOrder + 1)
                .build();

        product.getImages().add(image);
        productRepository.save(product);

        log.info("Product image added: productId={}, imageUrl={}, isPrimary={}", 
                productId, request.getImageUrl(), shouldBePrimary);
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

        log.info("Product primary image set: productId={}, imageId={}", productId, imageId);
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

        log.info("Product image deleted: productId={}, imageId={}", productId, imageId);
    }

    @Transactional(readOnly = true)
    public List<ProductImageResponse> getImagesByProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found"));

        return product.getImages().stream()
                .sorted((a, b) -> {
                    int orderA = a.getDisplayOrder() != null ? a.getDisplayOrder() : 0;
                    int orderB = b.getDisplayOrder() != null ? b.getDisplayOrder() : 0;
                    return Integer.compare(orderA, orderB);
                })
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

        log.info("Product image updated: productId={}, imageId={}", productId, imageId);
    }

    @Transactional
    public void reorderImages(Long productId, List<Long> imageIds) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found"));

        Map<Long, ProductImage> imageMap = product.getImages().stream()
                .collect(Collectors.toMap(ProductImage::getId, img -> img));

        for (Long imageId : imageIds) {
            if (!imageMap.containsKey(imageId)) {
                throw new BadRequestException("Image with id " + imageId + " does not belong to this product");
            }
        }

        for (int i = 0; i < imageIds.size(); i++) {
            ProductImage image = imageMap.get(imageIds.get(i));
            image.setDisplayOrder(i);
        }

        productImageRepository.saveAll(product.getImages());

        log.info("Product images reordered: productId={}, order={}", productId, imageIds);
    }
}


package com.riakgu.digilo.product;

import com.riakgu.digilo.common.exception.BadRequestException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.common.service.StorageService;
import com.riakgu.digilo.product.dto.ProductImageRequest;
import com.riakgu.digilo.product.dto.ProductImageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageService {

    private static final String IMAGE_PATH_PREFIX = "products/";
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final StorageService storageService;

    @Transactional
    public String uploadImage(Long productId, MultipartFile file) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found"));

        validateImageFile(file);

        String key = generateImageKey(productId, file);
        String imageUrl = storageService.upload(file, key);

        boolean shouldBePrimary = product.getImages().isEmpty();

        int maxOrder = product.getImages().stream()
                .mapToInt(img -> img.getDisplayOrder() != null ? img.getDisplayOrder() : 0)
                .max()
                .orElse(-1);

        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(imageUrl)
                .isPrimary(shouldBePrimary)
                .displayOrder(maxOrder + 1)
                .build();

        product.getImages().add(image);
        productRepository.save(product);

        log.info("Product image uploaded: productId={}, key={}", productId, key);

        return imageUrl;
    }

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

        String key = storageService.extractKey(image.getImageUrl());
        if (key != null) {
            storageService.delete(key);
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

    @Transactional(readOnly = true)
    public ProductImageResponse getById(Long productId, Long imageId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found"));

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Image with id " + imageId + " not found"));

        if (!image.getProduct().getId().equals(productId)) {
            throw new BadRequestException("Image does not belong to this product");
        }

        return ProductImageResponse.fromEntity(image);
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

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Only image files are allowed (JPEG, PNG, WebP, GIF)");
        }
    }

    private String generateImageKey(Long productId, MultipartFile file) {
        String extension = getExtension(file.getOriginalFilename(), file.getContentType());
        return IMAGE_PATH_PREFIX + productId + "/" + UUID.randomUUID() + "." + extension;
    }

    private String getExtension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        }

        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "bin";
        };
    }
}

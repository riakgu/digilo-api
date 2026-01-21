package com.riakgu.digilo.product;

import com.riakgu.digilo.category.Category;
import com.riakgu.digilo.category.CategoryRepository;
import com.riakgu.digilo.category.dto.CategoryResponse;
import com.riakgu.digilo.common.exception.BadRequestException;
import com.riakgu.digilo.common.exception.DuplicateResourceException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.common.util.SlugUtil;
import com.riakgu.digilo.product.dto.ProductImageRequest;
import com.riakgu.digilo.product.dto.ProductImageResponse;
import com.riakgu.digilo.product.dto.ProductRequest;
import com.riakgu.digilo.product.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;

    @Transactional
    public ProductResponse create(ProductRequest request) {

        String newName = request.getName().trim();
        String newSlug = SlugUtil.normalize(request.getSlug());

        if (productRepository.existsByName(newName)) {
            throw new DuplicateResourceException("Product with name " + newName + " already exists");
        }

        if (productRepository.existsBySlug(newSlug)) {
            throw new DuplicateResourceException("Product with slug " + newSlug + " already exists");
        }

        Product product = Product.builder()
                .name(newName)
                .slug(newSlug)
                .description(request.getDescription())
                .isActive(true)
                .build();

        productRepository.save(product);

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            for (Long categoryId : request.getCategoryIds()) {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new NotFoundException("Category with id " + categoryId + " not found"));

                ProductCategory productCategory = new ProductCategory();
                ProductCategoryId pcId = new ProductCategoryId(null, categoryId);
                productCategory.setId(pcId);
                productCategory.setProduct(product);
                productCategory.setCategory(category);

                product.getCategories().add(productCategory);
            }
        }

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .imageUrl(request.getImageUrls().get(i))
                        .isPrimary(i == 0)
                        .build();

                product.getImages().add(image);
            }
        }

        return ProductResponse.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getActiveBySlug(String slug) {
        Product product = productRepository.findBySlugAndIsActive(slug, Boolean.TRUE)
                .orElseThrow(() -> new NotFoundException("Product with slug " + slug + " not found")) ;

        return ProductResponse.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product= productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product with id " + id + " not found"));

        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse update(ProductRequest request, Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product with id " + id + " not found"));

        String newName = request.getName().trim();
        String newSlug = SlugUtil.normalize(request.getSlug());

        if (!product.getName().equalsIgnoreCase(newName)) {
            if (productRepository.existsByName(newName)) {
                throw new DuplicateResourceException("Product with name " + newName + " already exists");
            }
        }

        if (!product.getSlug().equalsIgnoreCase(newSlug)) {
            if (productRepository.existsBySlug(newSlug)) {
                throw new DuplicateResourceException("Product with slug " + newSlug + " already exists");
            }
        }

        product.setName(newName);
        product.setSlug(newSlug);
        product.setDescription(request.getDescription());

        productRepository.save(product);

        product.getCategories().clear();

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            for (Long categoryId : request.getCategoryIds()) {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new NotFoundException("Category with id " + categoryId + " not found"));

                ProductCategory productCategory = new ProductCategory();
                ProductCategoryId pcId = new ProductCategoryId(product.getId(), categoryId);
                productCategory.setId(pcId);
                productCategory.setProduct(product);
                productCategory.setCategory(category);

                product.getCategories().add(productCategory);
            }
        }

        product.getImages().clear();
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .imageUrl(request.getImageUrls().get(i))
                        .isPrimary(i == 0)
                        .build();

                product.getImages().add(image);
            }
        }

        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public void updateStatus(Long id, boolean isActive) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product with id " + id + " not found"));

        product.setIsActive(isActive);
        productRepository.save(product);

    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllActive(Pageable pageable) {
        return productRepository.findAllByIsActive(true, pageable)
                .map(ProductResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(ProductResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String query, Pageable pageable) {
        return productRepository.searchProducts(query, pageable)
                .map(ProductResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> getCategoriesByProduct(String slug, Pageable pageable) {
        productRepository.findBySlugAndIsActive(slug, true)
                .orElseThrow(() -> new NotFoundException("Product with slug " + slug + " not found"));

        return categoryRepository.findAllByProductsProductSlugAndIsActive(slug, true, pageable)
                .map(CategoryResponse::fromEntity);
    }

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

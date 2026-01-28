package com.riakgu.digilo.product;

import com.riakgu.digilo.category.Category;
import com.riakgu.digilo.category.CategoryRepository;
import com.riakgu.digilo.category.dto.CategoryResponse;
import com.riakgu.digilo.common.exception.BadRequestException;
import com.riakgu.digilo.common.exception.DuplicateResourceException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.common.util.SlugUtil;
import com.riakgu.digilo.product.dto.*;
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
    private final ProductInventoryRepository inventoryRepository;
    private final ProductImageHelper productImageHelper;

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

        List<ProductVariantResponse> variants = product.getVariants().stream()
                .map(variant -> {
                    long stock = inventoryRepository.countByVariantIdAndStatus(variant.getId(), InventoryStatus.AVAILABLE);
                    return ProductVariantResponse.fromEntity(variant, stock);
                })
                .collect(Collectors.toList());

        return ProductResponse.fromEntity(product, variants, productImageHelper.getDisplayImageUrl(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse getActiveBySlug(String slug) {
        Product product = productRepository.findBySlugAndIsActive(slug, Boolean.TRUE)
                .orElseThrow(() -> new NotFoundException("Product with slug " + slug + " not found"));

        List<ProductVariantResponse> variantsWithStock = product.getVariants().stream()
                .filter(ProductVariant::getIsActive)
                .map(variant -> {
                    long stock = inventoryRepository.countByVariantIdAndStatus(
                            variant.getId(), InventoryStatus.AVAILABLE);
                    return ProductVariantResponse.fromEntity(variant, stock);
                })
                .collect(Collectors.toList());

        return ProductResponse.fromEntity(product, variantsWithStock, productImageHelper.getDisplayImageUrl(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product= productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product with id " + id + " not found"));

        List<ProductVariantResponse> variants = product.getVariants().stream()
                .map(variant -> {
                    long stock = inventoryRepository.countByVariantIdAndStatus(variant.getId(), InventoryStatus.AVAILABLE);
                    return ProductVariantResponse.fromEntity(variant, stock);
                })
                .collect(Collectors.toList());

        return ProductResponse.fromEntity(product, variants, productImageHelper.getDisplayImageUrl(product));
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

        List<ProductVariantResponse> variants = product.getVariants().stream()
                .map(variant -> {
                    long stock = inventoryRepository.countByVariantIdAndStatus(variant.getId(), InventoryStatus.AVAILABLE);
                    return ProductVariantResponse.fromEntity(variant, stock);
                })
                .collect(Collectors.toList());

        return ProductResponse.fromEntity(product, variants, productImageHelper.getDisplayImageUrl(product));
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
                .map(product -> {
                    List<ProductVariantResponse> variants = product.getVariants().stream()
                            .map(variant -> {
                                long stock = inventoryRepository.countByVariantIdAndStatus(variant.getId(), InventoryStatus.AVAILABLE);
                                return ProductVariantResponse.fromEntity(variant, stock);
                            })
                            .collect(Collectors.toList());
                    return ProductResponse.fromEntity(product, variants, productImageHelper.getDisplayImageUrl(product));
                });
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(product -> {
                    List<ProductVariantResponse> variants = product.getVariants().stream()
                            .map(variant -> {
                                long stock = inventoryRepository.countByVariantIdAndStatus(variant.getId(), InventoryStatus.AVAILABLE);
                                return ProductVariantResponse.fromEntity(variant, stock);
                            })
                            .collect(Collectors.toList());
                    return ProductResponse.fromEntity(product, variants, productImageHelper.getDisplayImageUrl(product));
                });
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String query, Pageable pageable) {
        return productRepository.searchProducts(query, pageable)
                .map(product -> {
                    List<ProductVariantResponse> variants = product.getVariants().stream()
                            .map(variant -> {
                                long stock = inventoryRepository.countByVariantIdAndStatus(variant.getId(), InventoryStatus.AVAILABLE);
                                return ProductVariantResponse.fromEntity(variant, stock);
                            })
                            .collect(Collectors.toList());
                    return ProductResponse.fromEntity(product, variants, productImageHelper.getDisplayImageUrl(product));
                });
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> getCategoriesByProduct(String slug, Pageable pageable) {
        productRepository.findBySlugAndIsActive(slug, true)
                .orElseThrow(() -> new NotFoundException("Product with slug " + slug + " not found"));

        return categoryRepository.findAllByProductsProductSlugAndIsActive(slug, true, pageable)
                .map(CategoryResponse::fromEntity);
    }

}

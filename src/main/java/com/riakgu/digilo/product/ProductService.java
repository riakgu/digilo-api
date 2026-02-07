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
import java.util.Map;
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
                    return ProductVariantResponse.fromEntity(variant, stock, productImageHelper.getDisplayImageUrl(product));
                })
                .collect(Collectors.toList());

        log.info("Product created: id={}, name={}, slug={}", product.getId(), newName, newSlug);

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
                    return ProductVariantResponse.fromEntity(variant, stock, productImageHelper.getDisplayImageUrl(product));
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
                    return ProductVariantResponse.fromEntity(variant, stock, productImageHelper.getDisplayImageUrl(product));
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

        // Update active status if provided
        if (request.getIsActive() != null) {
            product.setIsActive(request.getIsActive());
        }

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
                    return ProductVariantResponse.fromEntity(variant, stock, productImageHelper.getDisplayImageUrl(product));
                })
                .collect(Collectors.toList());

        log.info("Product updated: id={}, name={}, slug={}", id, newName, newSlug);

        return ProductResponse.fromEntity(product, variants, productImageHelper.getDisplayImageUrl(product));
    }
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product with id " + id + " not found"));

        // Check if any variant has non-available inventory (reserved or sold)
        boolean hasActiveInventory = product.getVariants().stream()
                .anyMatch(variant -> inventoryRepository.countByVariantIdAndStatus(
                        variant.getId(), InventoryStatus.RESERVED) > 0 ||
                        inventoryRepository.countByVariantIdAndStatus(
                        variant.getId(), InventoryStatus.SOLD) > 0);

        if (hasActiveInventory) {
            throw new BadRequestException("Cannot delete product with reserved or sold inventory");
        }

        productRepository.delete(product);
        log.info("Product deleted: id={}, name={}", id, product.getName());
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllActive(String categorySlug, ProductSortOption sortKey, Boolean reverse, Pageable pageable) {
        Page<Product> products;

        boolean hasCategory = categorySlug != null && !categorySlug.isBlank();
        boolean isReverse = Boolean.TRUE.equals(reverse);

        if (hasCategory) {
            categoryRepository.findBySlugAndIsActive(categorySlug, true)
                    .orElseThrow(() -> new NotFoundException("Category not found: " + categorySlug));
            
            if (sortKey == null || sortKey == ProductSortOption.RELEVANCE) {
                products = productRepository.findAllByCategoriesCategorySlugAndIsActive(categorySlug, true, pageable);
            } else {
                products = switch (sortKey) {
                    case BEST_SELLING -> productRepository.findByCategoryOrderByTrending(categorySlug, pageable);
                    case CREATED_AT -> isReverse 
                            ? productRepository.findByCategoryOrderByLatest(categorySlug, pageable)
                            : productRepository.findByCategoryOrderByOldest(categorySlug, pageable);
                    case PRICE -> isReverse 
                            ? productRepository.findByCategoryOrderByPriceDesc(categorySlug, pageable)
                            : productRepository.findByCategoryOrderByPriceAsc(categorySlug, pageable);
                    default -> productRepository.findAllByCategoriesCategorySlugAndIsActive(categorySlug, true, pageable);
                };
            }
        } else if (sortKey == null || sortKey == ProductSortOption.RELEVANCE) {
            products = productRepository.findAllByIsActive(true, pageable);
        } else {
            products = switch (sortKey) {
                case BEST_SELLING -> productRepository.findAllActiveOrderByTrending(pageable);
                case CREATED_AT -> isReverse 
                        ? productRepository.findAllActiveOrderByLatest(pageable)
                        : productRepository.findAllActiveOrderByOldest(pageable);
                case PRICE -> isReverse 
                        ? productRepository.findAllActiveOrderByPriceDesc(pageable)
                        : productRepository.findAllActiveOrderByPriceAsc(pageable);
                default -> productRepository.findAllByIsActive(true, pageable);
            };
        }

        // Batch fetch stock for all variants across all products
        Map<Long, Long> stockMap = getStockMapForProducts(products.getContent());

        return products.map(product -> {
            String imageUrl = productImageHelper.getDisplayImageUrl(product);
            List<ProductVariantResponse> variants = product.getVariants().stream()
                    .map(variant -> ProductVariantResponse.fromEntity(
                            variant, stockMap.getOrDefault(variant.getId(), 0L), imageUrl))
                    .collect(Collectors.toList());
            return ProductResponse.fromEntity(product, variants, imageUrl);
        });
    }

    private Map<Long, Long> getStockMapForProducts(List<Product> products) {
        List<Long> allVariantIds = products.stream()
                .flatMap(p -> p.getVariants().stream())
                .map(ProductVariant::getId)
                .collect(Collectors.toList());

        if (allVariantIds.isEmpty()) {
            return Map.of();
        }

        return inventoryRepository.countByVariantIdsAndStatus(allVariantIds, InventoryStatus.AVAILABLE)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(String search, Boolean isActive, Pageable pageable) {
        Page<Product> products = productRepository.findAllWithFilters(search, isActive, pageable);
        Map<Long, Long> stockMap = getStockMapForProducts(products.getContent());

        return products.map(product -> {
            String imageUrl = productImageHelper.getDisplayImageUrl(product);
            List<ProductVariantResponse> variants = product.getVariants().stream()
                    .map(variant -> ProductVariantResponse.fromEntity(
                            variant, stockMap.getOrDefault(variant.getId(), 0L), imageUrl))
                    .collect(Collectors.toList());
            return ProductResponse.fromEntity(product, variants, imageUrl);
        });
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String query, Pageable pageable) {
        Page<Product> products = productRepository.searchProducts(query, pageable);
        Map<Long, Long> stockMap = getStockMapForProducts(products.getContent());

        return products.map(product -> {
            String imageUrl = productImageHelper.getDisplayImageUrl(product);
            List<ProductVariantResponse> variants = product.getVariants().stream()
                    .map(variant -> ProductVariantResponse.fromEntity(
                            variant, stockMap.getOrDefault(variant.getId(), 0L), imageUrl))
                    .collect(Collectors.toList());
            return ProductResponse.fromEntity(product, variants, imageUrl);
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

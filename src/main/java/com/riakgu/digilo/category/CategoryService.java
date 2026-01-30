package com.riakgu.digilo.category;

import com.riakgu.digilo.category.dto.CategoryRequest;
import com.riakgu.digilo.category.dto.CategoryResponse;
import com.riakgu.digilo.common.exception.DuplicateResourceException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.common.util.SlugUtil;
import com.riakgu.digilo.product.InventoryStatus;
import com.riakgu.digilo.product.ProductInventoryRepository;
import com.riakgu.digilo.product.ProductRepository;
import com.riakgu.digilo.product.dto.ProductResponse;
import com.riakgu.digilo.product.dto.ProductVariantResponse;
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
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductInventoryRepository inventoryRepository;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {

        String newName = request.getName().trim();
        String newSlug = SlugUtil.normalize(request.getSlug());

        if (categoryRepository.existsByName(newName)) {
            throw new DuplicateResourceException("Category with name " + newName + " already exists");
        }

        if (categoryRepository.existsBySlug(newSlug)) {
            throw new DuplicateResourceException("Category with slug " + newSlug + " already exists");
        }

        Category category = Category.builder()
                .name(newName)
                .slug(newSlug)
                .description(request.getDescription())
                .isActive(true)
                .build();

        categoryRepository.save(category);

        log.info("Category created: id={}, name={}, slug={}", category.getId(), newName, newSlug);

        return CategoryResponse.fromEntity(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getActiveBySlug(String slug) {
        Category category = categoryRepository.findBySlugAndIsActive(slug, Boolean.TRUE)
                .orElseThrow(() -> new NotFoundException("Category with slug " + slug + " not found")) ;

        return CategoryResponse.fromEntity(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category category= categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category with id " + id + " not found"));

        return CategoryResponse.fromEntity(category);
    }

    @Transactional
    public CategoryResponse update(CategoryRequest request, Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category with id " + id + " not found"));

        String newName = request.getName().trim();
        String newSlug = SlugUtil.normalize(request.getSlug());

        if (!category.getName().equalsIgnoreCase(newName)) {
            if (categoryRepository.existsByName(newName)) {
                throw new DuplicateResourceException("Category with name " + newName + " already exists");
            }
        }

        if (!category.getSlug().equalsIgnoreCase(newSlug)) {
            if (categoryRepository.existsBySlug(newSlug)) {
                throw new DuplicateResourceException("Category with slug " + newSlug + " already exists");
            }
        }

        category.setName(newName);
        category.setSlug(newSlug);
        category.setDescription(request.getDescription());

        // Update status if provided
        if (request.getActive() != null) {
            category.setIsActive(request.getActive());
        }

        categoryRepository.save(category);

        log.info("Category updated: id={}, name={}, slug={}, active={}", id, newName, newSlug, category.getIsActive());

        return CategoryResponse.fromEntity(category);
    }


    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAllActive(Pageable pageable) {
        return categoryRepository.findAllByIsActive(true, pageable)
                .map(CategoryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAll(Pageable pageable) {
        return categoryRepository.findAll(pageable)
                .map(CategoryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(String slug, Pageable pageable) {
        categoryRepository.findBySlugAndIsActive(slug, true)
                .orElseThrow(() -> new NotFoundException("Category with slug " + slug + " not found"));

        return productRepository.findAllByCategoriesCategorySlugAndIsActive(slug, true, pageable)
                .map(product -> {
                    List<ProductVariantResponse> variants = product.getVariants().stream()
                            .map(variant -> {
                                long stock = inventoryRepository.countByVariantIdAndStatus(variant.getId(), InventoryStatus.AVAILABLE);
                                return ProductVariantResponse.fromEntity(variant, stock);
                            })
                            .collect(Collectors.toList());
                    return ProductResponse.fromEntity(product, variants);
                });
    }
}

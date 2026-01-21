package com.riakgu.digilo.product;

import com.riakgu.digilo.common.exception.DuplicateResourceException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.common.util.SlugUtil;
import com.riakgu.digilo.product.dto.ProductRequest;
import com.riakgu.digilo.product.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;

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
}

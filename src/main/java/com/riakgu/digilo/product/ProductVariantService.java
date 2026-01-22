package com.riakgu.digilo.product;

import com.riakgu.digilo.common.exception.DuplicateResourceException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.product.dto.ProductVariantRequest;
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
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final ProductInventoryRepository inventoryRepository;

    @Transactional
    public ProductVariantResponse create(Long productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found"));

        if (productVariantRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Variant with SKU " + request.getSku() + " already exists");
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(request.getSku())
                .name(request.getName())
                .price(request.getPrice())
                .deliveryType(request.getDeliveryType())
                .durationDays(request.getDurationDays())
                .warrantyDays(request.getWarrantyDays())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .metadata(request.getMetadata())
                .build();

        productVariantRepository.save(variant);
        product.getVariants().add(variant);

        long stock = inventoryRepository.countByVariantIdAndStatus(variant.getId(), InventoryStatus.AVAILABLE);
        return ProductVariantResponse.fromEntity(variant, stock);
    }

    @Transactional(readOnly = true)
    public ProductVariantResponse getById(Long id) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Variant with id " + id + " not found"));

        long stock = inventoryRepository.countByVariantIdAndStatus(variant.getId(), InventoryStatus.AVAILABLE);
        return ProductVariantResponse.fromEntity(variant, stock);
    }

    @Transactional(readOnly = true)
    public ProductVariantResponse getBySku(String sku) {
        ProductVariant variant = productVariantRepository.findBySku(sku)
                .orElseThrow(() -> new NotFoundException("Variant with SKU " + sku + " not found"));

        long stock = inventoryRepository.countByVariantIdAndStatus(variant.getId(), InventoryStatus.AVAILABLE);
        return ProductVariantResponse.fromEntity(variant, stock);
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getByProductId(Long productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found"));

        return productVariantRepository.findByProductId(productId).stream()
                .map(variant -> {
                    long stock = inventoryRepository.countByVariantIdAndStatus(variant.getId(), InventoryStatus.AVAILABLE);
                    return ProductVariantResponse.fromEntity(variant, stock);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ProductVariantResponse> getAll(Pageable pageable) {
        return productVariantRepository.findAll(pageable)
                .map(variant -> {
                    long stock = inventoryRepository.countByVariantIdAndStatus(variant.getId(), InventoryStatus.AVAILABLE);
                    return ProductVariantResponse.fromEntity(variant, stock);
                });
    }

    @Transactional
    public ProductVariantResponse update(Long id, ProductVariantRequest request) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Variant with id " + id + " not found"));

        if (!variant.getSku().equals(request.getSku())
                && productVariantRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Variant with SKU " + request.getSku() + " already exists");
        }

        variant.setSku(request.getSku());
        variant.setName(request.getName());
        variant.setPrice(request.getPrice());
        variant.setDeliveryType(request.getDeliveryType());
        variant.setDurationDays(request.getDurationDays());
        variant.setWarrantyDays(request.getWarrantyDays());
        variant.setMetadata(request.getMetadata());

        if (request.getIsActive() != null) {
            variant.setIsActive(request.getIsActive());
        }

        productVariantRepository.save(variant);

        long stock = inventoryRepository.countByVariantIdAndStatus(variant.getId(), InventoryStatus.AVAILABLE);
        return ProductVariantResponse.fromEntity(variant, stock);
    }

    @Transactional
    public void updateStatus(Long id, boolean isActive) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Variant with id " + id + " not found"));

        variant.setIsActive(isActive);
        productVariantRepository.save(variant);
    }

    @Transactional
    public void delete(Long id) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Variant with id " + id + " not found"));

        variant.getProduct().getVariants().remove(variant);
        productVariantRepository.delete(variant);
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getActiveByProductId(Long productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found"));

        return productVariantRepository.findByProductIdAndIsActive(productId, true).stream()
                .map(variant -> {
                    long stock = inventoryRepository.countByVariantIdAndStatus(variant.getId(), InventoryStatus.AVAILABLE);
                    return ProductVariantResponse.fromEntity(variant, stock);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getByProductIdWithStock(Long productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found"));

        return productVariantRepository.findByProductId(productId).stream()
                .map(variant -> {
                    long stock = inventoryRepository.countByVariantIdAndStatus(variant.getId(), InventoryStatus.AVAILABLE);
                    return ProductVariantResponse.fromEntity(variant, stock);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getActiveByProductIdWithStock(Long productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found"));

        return productVariantRepository.findByProductIdAndIsActive(productId, true).stream()
                .map(variant -> {
                    long stock = inventoryRepository.countByVariantIdAndStatus(variant.getId(), InventoryStatus.AVAILABLE);
                    return ProductVariantResponse.fromEntity(variant, stock);
                })
                .collect(Collectors.toList());
    }
}
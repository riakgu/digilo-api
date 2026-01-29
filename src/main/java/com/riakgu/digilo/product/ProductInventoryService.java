package com.riakgu.digilo.product;

import com.riakgu.digilo.common.exception.BadRequestException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.product.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductInventoryService {

    private final ProductInventoryRepository inventoryRepository;
    private final ProductVariantRepository variantRepository;
    private final EncryptionService encryptionService;

    @Transactional
    public ProductInventoryResponse create(ProductInventoryRequest request) {
        ProductVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new NotFoundException("Variant with id " + request.getVariantId() + " not found"));

        String encryptedCredential = encryptionService.encrypt(request.getCredential());

        ProductInventory inventory = ProductInventory.builder()
                .variant(variant)
                .credential(encryptedCredential)
                .status(InventoryStatus.AVAILABLE)
                .build();

        inventoryRepository.save(inventory);

        log.info("Inventory created: id={}, variantId={}, status={}",
                inventory.getId(), variant.getId(), inventory.getStatus());

        return ProductInventoryResponse.fromEntity(inventory);
    }

    @Transactional
    public List<ProductInventoryResponse> createBulk(ProductInventoryBulkRequest request) {
        ProductVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new NotFoundException("Variant with id " + request.getVariantId() + " not found"));

        List<ProductInventoryResponse> results = request.getCredentials().stream()
                .map(credential -> {
                    String encrypted = encryptionService.encrypt(credential);
                    ProductInventory inventory = ProductInventory.builder()
                            .variant(variant)
                            .credential(encrypted)
                            .status(InventoryStatus.AVAILABLE)
                            .build();
                    inventoryRepository.save(inventory);
                    return ProductInventoryResponse.fromEntity(inventory);
                })
                .collect(Collectors.toList());

        log.info("Bulk inventory created: variantId={}, count={}", request.getVariantId(), results.size());

        return results;
    }

    @Transactional(readOnly = true)
    public Page<ProductInventoryResponse> getAll(Pageable pageable) {
        return inventoryRepository.findAll(pageable)
                .map(inventory -> ProductInventoryResponse.fromEntity(inventory));
    }

    @Transactional(readOnly = true)
    public ProductInventoryResponse getById(Long id) {
        ProductInventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inventory with id " + id + " not found"));

        return ProductInventoryResponse.fromEntity(inventory);
    }

    @Transactional(readOnly = true)
    public ProductInventoryResponse getByIdWithCredential(Long id) {
        ProductInventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inventory with id " + id + " not found"));

        Map<String, Object> decrypted = encryptionService.decrypt(inventory.getCredential());

        return ProductInventoryResponse.fromEntityWithCredential(inventory, decrypted);
    }

    @Transactional(readOnly = true)
    public List<ProductInventoryResponse> getByVariantId(Long variantId) {
        variantRepository.findById(variantId)
                .orElseThrow(() -> new NotFoundException("Variant with id " + variantId + " not found"));

        return inventoryRepository.findByVariantId(variantId).stream()
                .map(ProductInventoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countAvailableStock(Long variantId) {
        return inventoryRepository.countByVariantIdAndStatus(variantId, InventoryStatus.AVAILABLE);
    }

    @Transactional
    public ProductInventoryResponse reserve(Long variantId, Long orderItemId) {
        ProductInventory inventory = inventoryRepository
                .findFirstByVariantIdAndStatus(variantId, InventoryStatus.AVAILABLE)
                .orElseThrow(() -> new BadRequestException("No available stock for variant " + variantId));

        inventory.setStatus(InventoryStatus.RESERVED);
        inventory.setOrderItemId(orderItemId);
        inventory.setReservedAt(Instant.now());

        inventoryRepository.save(inventory);

        return ProductInventoryResponse.fromEntity(inventory);
    }

    @Transactional
    public ProductInventoryResponse markAsSold(Long inventoryId) {
        ProductInventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventory with id " + inventoryId + " not found"));

        if (inventory.getStatus() != InventoryStatus.RESERVED) {
            throw new BadRequestException("Inventory must be RESERVED before marking as SOLD");
        }

        inventory.setStatus(InventoryStatus.SOLD);
        inventory.setSoldAt(Instant.now());

        inventoryRepository.save(inventory);

        return ProductInventoryResponse.fromEntity(inventory);
    }

    @Transactional
    public void releaseReservation(Long inventoryId) {
        ProductInventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventory with id " + inventoryId + " not found"));

        if (inventory.getStatus() != InventoryStatus.RESERVED) {
            throw new BadRequestException("Only RESERVED inventory can be released");
        }

        inventory.setStatus(InventoryStatus.AVAILABLE);
        inventory.setOrderItemId(null);
        inventory.setReservedAt(null);

        inventoryRepository.save(inventory);
    }

    @Transactional
    public ProductInventoryResponse update(Long id, ProductInventoryUpdateRequest request) {
        ProductInventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inventory with id " + id + " not found"));

        if (inventory.getStatus() != InventoryStatus.AVAILABLE) {
            throw new BadRequestException("Only AVAILABLE inventory can be updated");
        }

        // Validate and update variant if changed
        if (!inventory.getVariant().getId().equals(request.getVariantId())) {
            ProductVariant newVariant = variantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new NotFoundException("Variant with id " + request.getVariantId() + " not found"));
            inventory.setVariant(newVariant);
        }

        // Re-encrypt credential
        String encryptedCredential = encryptionService.encrypt(request.getCredential());
        inventory.setCredential(encryptedCredential);

        inventoryRepository.save(inventory);

        return ProductInventoryResponse.fromEntity(inventory);
    }

    @Transactional
    public void delete(Long id) {
        ProductInventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inventory with id " + id + " not found"));

        if (inventory.getStatus() != InventoryStatus.AVAILABLE) {
            throw new BadRequestException("Only AVAILABLE inventory can be deleted");
        }

        inventoryRepository.delete(inventory);

        log.info("Inventory deleted: id={}, variantId={}", id, inventory.getVariant().getId());
    }
}
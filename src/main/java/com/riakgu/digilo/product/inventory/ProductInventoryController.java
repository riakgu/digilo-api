package com.riakgu.digilo.product.inventory;

import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.product.inventory.dto.ProductInventoryBulkRequest;
import com.riakgu.digilo.product.inventory.dto.ProductInventoryRequest;
import com.riakgu.digilo.product.inventory.dto.ProductInventoryResponse;
import com.riakgu.digilo.product.inventory.dto.ProductInventoryUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductInventoryController {

    private final ProductInventoryService inventoryService;

    @PostMapping("/admin/inventories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductInventoryResponse>> create(
            @Valid @RequestBody ProductInventoryRequest request
    ) {
        ProductInventoryResponse response = inventoryService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("CREATED", "Inventory created successfully", response));
    }

    @GetMapping("/admin/inventories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProductInventoryResponse>>> getAll(
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) InventoryStatus status,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<ProductInventoryResponse> inventories = inventoryService.getAll(variantId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("OK", "Get all inventories successful", inventories));
    }

    @PostMapping("/admin/inventories/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProductInventoryResponse>>> createBulk(
            @Valid @RequestBody ProductInventoryBulkRequest request
            ) {
        List<ProductInventoryResponse> responses = inventoryService.createBulk(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("CREATED", "Bulk inventory created", responses));
    }

    @GetMapping("/admin/inventories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductInventoryResponse>> getById(
            @PathVariable Long id
    ) {
        ProductInventoryResponse response = inventoryService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("OK", "Inventory found", response));
    }

    @GetMapping("/admin/inventories/{id}/credential")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductInventoryResponse>> getByIdWithCredential(
            @PathVariable Long id
    ) {
        ProductInventoryResponse response = inventoryService.getByIdWithCredential(id);
        return ResponseEntity.ok(ApiResponse.success("OK", "Inventory with credential", response));
    }

    @PutMapping("/admin/inventories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductInventoryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductInventoryUpdateRequest request
    ) {
        ProductInventoryResponse response = inventoryService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Inventory updated successfully", response));
    }

    @GetMapping("/admin/variants/{variantId}/inventories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProductInventoryResponse>>> getByVariant(
            @PathVariable Long variantId
    ) {
        List<ProductInventoryResponse> responses = inventoryService.getByVariantId(variantId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Inventory list", responses));
    }

    @GetMapping("/admin/variants/{variantId}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getAvailableStock(
            @PathVariable Long variantId
    ) {
        long stock = inventoryService.countAvailableStock(variantId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Available stock", stock));
    }

    @GetMapping("/public/variants/{variantId}/stock")
    public ResponseEntity<ApiResponse<Long>> getPublicStock(
            @PathVariable Long variantId
    ) {
        long stock = inventoryService.countAvailableStock(variantId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Available stock", stock));
    }

    @PostMapping("/admin/inventories/{id}/sold")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductInventoryResponse>> markAsSold(
            @PathVariable Long id
    ) {
        ProductInventoryResponse response = inventoryService.markAsSold(id);
        return ResponseEntity.ok(ApiResponse.success("OK", "Inventory marked as sold", response));
    }

    @PostMapping("/admin/inventories/{id}/release")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> releaseReservation(
            @PathVariable Long id
    ) {
        inventoryService.releaseReservation(id);
        return ResponseEntity.ok(ApiResponse.success("OK", "Reservation released"));
    }

    @DeleteMapping("/admin/inventories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id
    ) {
        inventoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("OK", "Inventory deleted"));
    }
}
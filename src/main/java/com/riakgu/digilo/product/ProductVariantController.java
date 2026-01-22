package com.riakgu.digilo.product;

import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.product.dto.ProductVariantRequest;
import com.riakgu.digilo.product.dto.ProductVariantResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductVariantController {
    
    private final ProductVariantService productVariantService;

    @PostMapping("/admin/products/{productId}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> createVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        ProductVariantResponse variant = productVariantService.create(productId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("CREATED", "Variant created successfully", variant));
    }

    @GetMapping("/admin/products/{productId}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getVariantsByProduct(
            @PathVariable Long productId
    ) {
        List<ProductVariantResponse> variants = productVariantService.getByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Variants retrieved", variants));
    }

    @GetMapping("/admin/variants/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> getVariantById(
            @PathVariable Long id
    ) {
        ProductVariantResponse variant = productVariantService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("OK", "Variant found", variant));
    }

    @PutMapping("/admin/variants/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable Long id,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        ProductVariantResponse variant = productVariantService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Variant updated successfully", variant));
    }

    @PatchMapping("/admin/variants/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateVariant(@PathVariable Long id) {
        productVariantService.updateStatus(id, true);
        return ResponseEntity.ok(ApiResponse.success("OK", "Variant activated"));
    }

    @PatchMapping("/admin/variants/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateVariant(@PathVariable Long id) {
        productVariantService.updateStatus(id, false);
        return ResponseEntity.ok(ApiResponse.success("OK", "Variant deactivated"));
    }

    @DeleteMapping("/admin/variants/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(@PathVariable Long id) {
        productVariantService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("OK", "Variant deleted"));
    }

    @GetMapping("/public/products/{productId}/variants")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getActiveVariantsByProduct(
            @PathVariable Long productId
    ) {
        List<ProductVariantResponse> variants = productVariantService.getActiveByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Variants retrieved", variants));
    }

}

package com.riakgu.digilo.product;

import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.product.dto.ProductRequest;
import com.riakgu.digilo.product.dto.ProductResponse;
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
public class ProductController {

    private final ProductService productService;

    @GetMapping("/public/products/{slug}")
    public ResponseEntity<ApiResponse<ProductResponse>> getBySlug(
            @PathVariable String slug
    ) {
        ProductResponse product = productService.getActiveBySlug(slug);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Product found successfully", product));
    }

    @GetMapping("/public/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllActive(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<ProductResponse> products = productService.getAllActive(pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Get all active products successful", products));
    }

    @GetMapping("/public/products/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> search(
            @RequestParam String q,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<ProductResponse> products = productService.search(q, pageable);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Search results", products));
    }

    @PostMapping("/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody ProductRequest request
    ){
        ProductResponse product = productService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("CREATED", "Product created successfully", product));

    }

    @GetMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(
            @PathVariable Long id
    ) {
        ProductResponse product = productService.getById(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Product found successfully", product));
    }

    @PutMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        ProductResponse product = productService.update(request, id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Product updated successfully", product));
    }


    @PatchMapping("/admin/products/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> activate(
            @PathVariable Long id
    ) {
        productService.updateStatus(id, true);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Product activated successfully"));
    }

    @PatchMapping("/admin/products/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> deactivate(
            @PathVariable Long id
    ) {
        productService.updateStatus(id, false);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Product deactivated successfully"));
    }

    @GetMapping("/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAll(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<ProductResponse> products = productService.getAll(pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Get all products successful", products));
    }
}

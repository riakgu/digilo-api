package com.riakgu.digilo.category;

import com.riakgu.digilo.category.dto.CategoryRequest;
import com.riakgu.digilo.category.dto.CategoryResponse;
import com.riakgu.digilo.common.dto.ApiResponse;
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
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/public/categories/{slug}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getBySlug(
            @PathVariable String slug
    ) {
        CategoryResponse category = categoryService.getActiveBySlug(slug);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Category found successfully", category));
    }

    @GetMapping("/public/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllActive(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<CategoryResponse> categories = categoryService.getAllActive(pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Get all active categories successful", categories));
    }

    @GetMapping("/public/categories/{slug}/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByCategory(
            @PathVariable String slug,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<ProductResponse> products = categoryService.getProductsByCategory(slug, pageable);
        return ResponseEntity.ok(ApiResponse.success("OK", "Products found", products));
    }

    @PostMapping("/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CategoryRequest request
    ){
        CategoryResponse category = categoryService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("CREATED", "Category created successfully", category));

    }

    @GetMapping("/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(
            @PathVariable Long id
    ) {
        CategoryResponse category = categoryService.getById(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Category found successfully", category));
    }

    @PutMapping("/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
        CategoryResponse category = categoryService.update(request, id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Category updated successfully", category));
    }

    @GetMapping("/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<CategoryResponse> categories = categoryService.getAll(pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Get all categories successful", categories));
    }
}


package com.riakgu.digilo.category;

import com.riakgu.digilo.category.dto.CategoryRequest;
import com.riakgu.digilo.category.dto.CategoryResponse;
import com.riakgu.digilo.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

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

    @GetMapping("/public/categories/{slug}")
    public ResponseEntity<ApiResponse<CategoryResponse>> findBySlug(
            @PathVariable String slug
    ) {
        CategoryResponse category = categoryService.getBySlug(slug);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Category found successfully", category));
    }

    @GetMapping("/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> findById(
            @PathVariable Long id
    ) {
        CategoryResponse category = categoryService.getById(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("OK", "Category found successfully", category));
    }
}

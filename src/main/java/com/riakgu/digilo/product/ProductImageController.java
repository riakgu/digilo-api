package com.riakgu.digilo.product;

import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.product.dto.ProductImageRequest;
import com.riakgu.digilo.product.dto.ProductImageResponse;
import com.riakgu.digilo.product.dto.ReorderImagesRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductImageController {

    private final R2ImageService r2ImageService;
    private final ProductImageService productImageService;

    @PostMapping("/admin/products/{id}/images")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> addImage(
            @PathVariable Long id,
            @RequestBody ProductImageRequest request
    ) {
        productImageService.addImage(id, request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Image added"));
    }

    @PatchMapping("/admin/products/{productId}/images/{imageId}/primary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> setPrimaryImage(
            @PathVariable Long productId,
            @PathVariable Long imageId
    ) {
        productImageService.setPrimaryImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Primary image set"));
    }

    @DeleteMapping("/admin/products/{productId}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long productId,
            @PathVariable Long imageId
    ) {
        productImageService.deleteImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.success("OK", "Image deleted"));
    }

    @GetMapping("/public/products/{id}/images")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> getImages(
            @PathVariable Long id
    ) {
        List<ProductImageResponse> images = productImageService.getImagesByProduct(id);
        return ResponseEntity.ok(ApiResponse.success("OK", "Images retrieved successfully", images));
    }

    @PutMapping("/admin/products/{productId}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            @Valid @RequestBody ProductImageRequest request
    ) {
        productImageService.updateImage(productId, imageId, request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Image updated successfully"));
    }

    @PostMapping("/admin/products/{id}/images/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        String imageUrl = r2ImageService.uploadImage(file);

        ProductImageRequest request = new ProductImageRequest();
        request.setImageUrl(imageUrl);
        request.setIsPrimary(false);

        productImageService.addImage(id, request);

        return ResponseEntity.ok(ApiResponse.success("OK", "Image uploaded successfully", imageUrl));
    }

    @PatchMapping("/admin/products/{id}/images/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reorderImages(
            @PathVariable Long id,
            @Valid @RequestBody ReorderImagesRequest request
    ) {
        productImageService.reorderImages(id, request.getImageIds());
        return ResponseEntity.ok(ApiResponse.success("OK", "Images reordered successfully"));
    }
}


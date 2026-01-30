package com.riakgu.digilo.promo;

import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.promo.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PromoController {

    private final PromoService promoService;

    @PostMapping("/user/promos/validate")
    public ResponseEntity<ApiResponse<PromoValidationResponse>> validatePromo(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ApplyPromoRequest request
    ) {
        PromoValidationResponse result = promoService.validatePromo(userId, request.getCode());
        return ResponseEntity.ok(ApiResponse.success("OK", "Promo validation completed", result));
    }

    @PostMapping("/admin/promos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromoResponse>> create(
            @Valid @RequestBody PromoRequest request
    ) {
        PromoResponse promo = promoService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("CREATED", "Promo created successfully", promo));
    }

    @PutMapping("/admin/promos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromoResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody PromoRequest request
    ) {
        PromoResponse promo = promoService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("OK", "Promo updated successfully", promo));
    }

    @GetMapping("/admin/promos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromoResponse>> getById(
            @PathVariable Long id
    ) {
        PromoResponse promo = promoService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("OK", "Promo retrieved", promo));
    }

    @GetMapping("/admin/promos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PromoResponse>>> getAll(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<PromoResponse> promos = promoService.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.success("OK", "Promos retrieved", promos));
    }
}

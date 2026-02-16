package com.riakgu.digilo.product.inventory.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ProductInventoryUpdateRequest {

    @NotNull(message = "Variant ID is required")
    private Long variantId;

    @NotEmpty(message = "Credential is required")
    private Map<String, Object> credential;
}

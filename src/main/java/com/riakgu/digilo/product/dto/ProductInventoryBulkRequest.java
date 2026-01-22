package com.riakgu.digilo.product.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProductInventoryBulkRequest {

    @NotNull(message = "Variant ID is required")
    private Long variantId;

    @NotEmpty(message = "Credentials must not be empty")
    private List<@NotEmpty Map<String, Object>> credentials;
}


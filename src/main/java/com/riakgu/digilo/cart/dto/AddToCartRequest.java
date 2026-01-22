package com.riakgu.digilo.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToCartRequest {

    @NotNull(message = "Variant ID is required")
    private Long variantId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;

}
package com.riakgu.digilo.product.dto;

import com.riakgu.digilo.product.DeliveryType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class ProductVariantRequest {

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must be at most 100 characters")
    private String sku;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Delivery type is required")
    private DeliveryType deliveryType;

    @Min(value = 1, message = "Duration days must be at least 1")
    private Integer durationDays;

    @Min(value = 0, message = "Warranty days must be at least 0")
    private Integer warrantyDays;

    private Boolean isActive = true;

    private Map<String, Object> metadata;
}
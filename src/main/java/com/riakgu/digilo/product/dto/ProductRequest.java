package com.riakgu.digilo.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(min = 2, max = 100, message = "Slug must be between 2 and 100 characters")
    private String slug;

    private String description;

}


package com.riakgu.digilo.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(min = 2, max = 100, message = "Slug must be between 2 and 100 characters")
    private String slug;

    private String description;

    private Boolean active;  // optional, for status update
}


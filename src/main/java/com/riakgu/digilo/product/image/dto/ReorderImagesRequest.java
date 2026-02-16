package com.riakgu.digilo.product.image.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ReorderImagesRequest {

    @NotEmpty(message = "Image IDs are required")
    private List<Long> imageIds;
}

package com.riakgu.digilo.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignCredentialRequest {

    @NotNull(message = "Inventory ID is required")
    private Long inventoryId;
}

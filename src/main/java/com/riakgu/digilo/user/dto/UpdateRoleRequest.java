package com.riakgu.digilo.user.dto;

import com.riakgu.digilo.user.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRoleRequest {

    @NotNull(message = "Role is required")
    private Role role;

}
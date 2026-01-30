package com.riakgu.digilo.user.dto;

import com.riakgu.digilo.user.Role;
import com.riakgu.digilo.user.UserStatus;
import lombok.Data;

@Data
public class AdminUpdateUserRequest {
    private Role role;         // optional
    private UserStatus status; // optional
}

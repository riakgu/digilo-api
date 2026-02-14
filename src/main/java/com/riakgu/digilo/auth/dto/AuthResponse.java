package com.riakgu.digilo.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.riakgu.digilo.common.dto.ApiResponse;
import com.riakgu.digilo.user.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private UserResponse user;
    private String accessToken;
    private String refreshToken;

}

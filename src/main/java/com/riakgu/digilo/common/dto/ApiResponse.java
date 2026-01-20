package com.riakgu.digilo.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {

    private String message;
    private Object data;
    private LocalDateTime timestamp;

    public static ApiResponse success(String message) {
        return ApiResponse.builder()
                .message(message)
                .build();
    }

    public static ApiResponse success(Object data) {
        return ApiResponse.builder()
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

}
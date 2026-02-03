package com.riakgu.digilo.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.riakgu.digilo.security.RequestTracingFilter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private String code;
    private String message;
    private T data;
    private Object errors;
    private String path;
    private String traceId;
    private LocalDateTime timestamp;
    
    private Pagination pagination;

    public static <T> ApiResponse<T> success(String code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .traceId(RequestTracingFilter.getCurrentTraceId())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(String code, String message, T data) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .traceId(RequestTracingFilter.getCurrentTraceId())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<List<T>> success(String code, String message, Page<T> page) {
        return ApiResponse.<java.util.List<T>>builder()
                .code(code)
                .message(message)
                .data(page.getContent())
                .pagination(Pagination.from(page))
                .traceId(RequestTracingFilter.getCurrentTraceId())
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> ApiResponse<T> error(String code, String message, String path) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .path(path)
                .traceId(RequestTracingFilter.getCurrentTraceId())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message, Object errors, String path) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .errors(errors)
                .path(path)
                .traceId(RequestTracingFilter.getCurrentTraceId())
                .timestamp(LocalDateTime.now())
                .build();
    }

}

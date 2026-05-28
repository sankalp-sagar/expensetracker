package com.sankalp.expensetracker.common.dto;

import java.time.Instant;
import java.util.List;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        List<String> errors,
        String traceId,
        Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> fail(String message, List<String> errors) {
        return new ApiResponse<>(false, null, message, errors, null, Instant.now());
    }
}

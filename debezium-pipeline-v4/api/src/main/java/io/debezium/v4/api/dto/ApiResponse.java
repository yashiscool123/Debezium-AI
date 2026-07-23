package io.debezium.v4.api.dto;

import java.time.Instant;
import java.util.Map;

public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    Map<String, Object> metadata,
    Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Success", data, Map.of(), Instant.now());
    }
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, Map.of(), Instant.now());
    }
    public static <T> ApiResponse<T> ok(T data, Map<String, Object> metadata) {
        return new ApiResponse<>(true, "Success", data, metadata, Instant.now());
    }
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, Map.of(), Instant.now());
    }
    public static <T> ApiResponse<T> error(String message, Map<String, Object> metadata) {
        return new ApiResponse<>(false, message, null, metadata, Instant.now());
    }
}

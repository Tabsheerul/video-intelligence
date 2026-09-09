package com.videoai.video_intelligence.dto;

import java.time.ZonedDateTime;

import lombok.Data;

/**
 * Standard API Response Wrapper
 * Ensures all REST APIs return a consistent JSON structure.
 */
@Data
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private ZonedDateTime timestamp;

    // Constructors
    public ApiResponse() {
        this.timestamp = ZonedDateTime.now();
    }

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = ZonedDateTime.now();
    }

    // Static helper methods for clean instantiation
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Operation successful", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

}

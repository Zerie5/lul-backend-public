package com.lul.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API response wrapper for standardizing all API responses.
 * This class provides a consistent structure for all API responses,
 * including success status, message, and data payload.
 *
 * @param <T> The type of data being returned in the response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * Indicates whether the API request was successful
     */
    private boolean success;

    /**
     * A message describing the result of the API request
     */
    private String message;

    /**
     * The data payload of the response
     */
    private T data;

    /**
     * Error code, if applicable (only included when success is false)
     */
    private String errorCode;

    /**
     * Constructor for successful responses with data
     *
     * @param success Whether the request was successful
     * @param message A message describing the result
     * @param data    The data payload
     */
    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /**
     * Constructor for error responses with an error code
     *
     * @param success   Whether the request was successful (typically false)
     * @param message   A message describing the error
     * @param errorCode The error code
     */
    public ApiResponse(boolean success, String message, String errorCode) {
        this.success = success;
        this.message = message;
        this.errorCode = errorCode;
    }

    /**
     * Static factory method for creating a successful response
     *
     * @param message A message describing the successful result
     * @param data    The data payload
     * @param <T>     The type of data being returned
     * @return A new ApiResponse instance
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * Static factory method for creating an error response
     *
     * @param message   A message describing the error
     * @param errorCode The error code
     * @param <T>       The type of data being returned (typically Void)
     * @return A new ApiResponse instance
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, message, errorCode);
    }
} 
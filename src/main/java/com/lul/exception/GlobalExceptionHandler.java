package com.lul.exception;

import com.lul.constant.ErrorCode;
import com.lul.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the application.
 * This class handles exceptions thrown by controllers and services,
 * converting them into standardized API responses.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle LulPayException
     *
     * @param ex The exception
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(LulPayException.class)
    public ResponseEntity<ApiResponse<Void>> handleLulPayException(LulPayException ex) {
        log.error("LulPayException: {}", ex.getMessage(), ex);
        
        ApiResponse<Void> response = new ApiResponse<>(
            false,
            ex.getMessage(),
            ex.getErrorCode().getCode()
        );
        
        return new ResponseEntity<>(response, ex.getErrorCode().getHttpStatus());
    }

    /**
     * Handle InsufficientFundsException
     *
     * @param ex The exception
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientFundsException(InsufficientFundsException ex) {
        log.error("InsufficientFundsException: {}", ex.getMessage(), ex);
        
        ApiResponse<Void> response = new ApiResponse<>(
            false,
            "Insufficient funds in your wallet",
            ErrorCode.INSUFFICIENT_FUNDS.getCode()
        );
        
        return new ResponseEntity<>(response, ErrorCode.INSUFFICIENT_FUNDS.getHttpStatus());
    }

    /**
     * Handle InvalidPinException
     *
     * @param ex The exception
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(InvalidPinException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPinException(InvalidPinException ex) {
        log.error("InvalidPinException: {}", ex.getMessage(), ex);
        
        ApiResponse<Void> response = new ApiResponse<>(
            false,
            "Invalid PIN provided",
            ErrorCode.INVALID_PIN.getCode()
        );
        
        return new ResponseEntity<>(response, ErrorCode.INVALID_PIN.getHttpStatus());
    }

    /**
     * Handle NotFoundException
     *
     * @param ex The exception
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(NotFoundException ex) {
        log.error("NotFoundException: {}", ex.getMessage(), ex);
        
        ApiResponse<Void> response = new ApiResponse<>(
            false,
            ex.getMessage(),
            ex.getErrorCode().getCode()
        );
        
        return new ResponseEntity<>(response, ex.getErrorCode().getHttpStatus());
    }

    /**
     * Handle validation exceptions
     *
     * @param ex The exception
     * @return ResponseEntity with validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage(), ex);
        
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value"
            ));
        
        ApiResponse<Map<String, String>> response = new ApiResponse<>(
            false,
            "Validation failed",
            errors
        );
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle constraint violation exceptions
     *
     * @param ex The exception
     * @return ResponseEntity with validation error details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(ConstraintViolationException ex) {
        log.error("Constraint violation: {}", ex.getMessage(), ex);
        
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            String field = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
            errors.put(field, violation.getMessage());
        });
        
        ApiResponse<Map<String, String>> response = new ApiResponse<>(
            false,
            "Validation failed",
            errors
        );
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle all other exceptions
     *
     * @param ex The exception
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponse<Void>> handleAllExceptions(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        
        ApiResponse<Void> response = new ApiResponse<>(
            false,
            "An unexpected error occurred",
            ErrorCode.SERVER_ERROR.getCode()
        );
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
} 
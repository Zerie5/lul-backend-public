package com.lul.exception;

import com.lul.constant.ErrorCode;
import lombok.Getter;

/**
 * Custom exception class for LulPay application.
 * This exception is used to represent business logic errors with specific error codes.
 */
@Getter
public class LulPayException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String message;

    /**
     * Constructs a new LulPayException with the specified error code and message.
     *
     * @param errorCode the error code associated with this exception
     * @param message   the detail message
     */
    public LulPayException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
    }

    /**
     * Constructs a new LulPayException with the specified error code, message, and cause.
     *
     * @param errorCode the error code associated with this exception
     * @param message   the detail message
     * @param cause     the cause of this exception
     */
    public LulPayException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.message = message;
    }

    /**
     * Constructs a new LulPayException with the specified error code.
     * The message will be set to the error code's name.
     *
     * @param errorCode the error code associated with this exception
     */
    public LulPayException(ErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.message = errorCode.name();
    }
} 
package com.lul.exception;

import com.lul.constant.ErrorCode;
import org.springframework.http.HttpStatus;

public class RegistrationException extends RuntimeException {
    private final ErrorCode errorCode;

    public RegistrationException(ErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
    }

    public RegistrationException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return errorCode.getHttpStatus();
    }
} 
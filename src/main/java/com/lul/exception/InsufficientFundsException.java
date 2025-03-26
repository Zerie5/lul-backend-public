package com.lul.exception;

import com.lul.constant.ErrorCode;
import lombok.Getter;

@Getter
public class InsufficientFundsException extends RuntimeException {
    private final ErrorCode errorCode;

    public InsufficientFundsException(ErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
    }

    public InsufficientFundsException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
} 
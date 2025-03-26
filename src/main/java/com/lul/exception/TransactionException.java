package com.lul.exception;

import com.lul.constant.ErrorCode;
import lombok.Getter;

@Getter
public class TransactionException extends RuntimeException {
    private final ErrorCode errorCode;

    public TransactionException(ErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
    }

    public TransactionException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
} 
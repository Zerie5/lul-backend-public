package com.lul.exception;

import com.lul.constant.ErrorCode;
import lombok.Getter;

/**
 * Exception thrown when a transaction exceeds the user's transaction limits
 */
@Getter
public class TransactionLimitExceededException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    public TransactionLimitExceededException(ErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }
    
    public TransactionLimitExceededException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
} 
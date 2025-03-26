package com.lul.exception;

import com.lul.constant.ErrorCode;

public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(ErrorCode errorCode) {
        super(errorCode.getCode());
    }

    public TooManyRequestsException(String message) {
        super(message);
    }
} 
package com.lul.exception;

import com.lul.constant.ErrorCode;
import lombok.Getter;

@Getter
public class InvalidPinException extends RuntimeException {
    private final ErrorCode errorCode;

    public InvalidPinException(ErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
    }

    public InvalidPinException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
} 
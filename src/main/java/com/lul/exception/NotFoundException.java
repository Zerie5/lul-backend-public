package com.lul.exception;

import com.lul.constant.ErrorCode;

public class NotFoundException extends BaseException {
    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public NotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
} 
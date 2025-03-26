package com.lul.exception;

import com.lul.constant.ErrorCode;
import lombok.Getter;

@Getter
public class ServiceException extends BaseException {
    public ServiceException(ErrorCode errorCode) {
        super(errorCode);
    }
} 
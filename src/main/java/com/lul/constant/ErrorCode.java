package com.lul.constant;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // Connection errors
    DATABASE_ERROR("ERR_003", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVER_ERROR("ERR_002", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // Duplicate errors
    DUPLICATE_EMAIL("ERR_101", HttpStatus.CONFLICT),
    DUPLICATE_USERNAME("ERR_102", HttpStatus.CONFLICT),
    DUPLICATE_PHONE("ERR_103", HttpStatus.CONFLICT),
    DUPLICATE_USER_ID("ERR_104", HttpStatus.CONFLICT),
    
    // Missing field errors
    MISSING_EMAIL("ERR_201", HttpStatus.BAD_REQUEST),
    MISSING_USERNAME("ERR_202", HttpStatus.BAD_REQUEST),
    MISSING_FIRST_NAME("ERR_203", HttpStatus.BAD_REQUEST),
    MISSING_LAST_NAME("ERR_204", HttpStatus.BAD_REQUEST),
    MISSING_PHONE("ERR_205", HttpStatus.BAD_REQUEST),
    MISSING_PASSWORD("ERR_206", HttpStatus.BAD_REQUEST),
    
    // Validation errors
    INVALID_EMAIL_FORMAT("ERR_301", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_FORMAT("ERR_302", HttpStatus.BAD_REQUEST),
    INVALID_USERNAME_FORMAT("ERR_303", HttpStatus.BAD_REQUEST),
    PASSWORD_TOO_SHORT("ERR_304", HttpStatus.BAD_REQUEST),
    PASSWORD_FORMAT("ERR_305", HttpStatus.BAD_REQUEST),
    
    // Email related errors
    EMAIL_SEND_FAILED("ERR_401", HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_TEMPLATE_ERROR("ERR_402", HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_AUTH_ERROR("ERR_403", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // Profile errors
    USER_NOT_FOUND("ERR_501", HttpStatus.NOT_FOUND),
    INVALID_TOKEN("ERR_502", HttpStatus.UNAUTHORIZED),
    PROFILE_FETCH_ERROR("ERR_503", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // Registration specific errors (600 series)
    DUPLICATE_USER("ERR_601", HttpStatus.CONFLICT),
    REGISTRATION_FAILED("ERR_602", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // PIN related errors (650 series)
    PIN_VERIFICATION_FAILED("ERR_651", HttpStatus.UNAUTHORIZED),
    PIN_NOT_SET("ERR_652", HttpStatus.BAD_REQUEST),
    PIN_UPDATE_FAILED("ERR_653", HttpStatus.INTERNAL_SERVER_ERROR),
    PIN_CREATION_FAILED("ERR_654", HttpStatus.UNAUTHORIZED),
    PIN_VERIFICATION_CONNECTIVITY_ERROR("ERR_655", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_PIN("ERR_651", HttpStatus.UNAUTHORIZED),
    PIN_ALREADY_SET("ERR_653", HttpStatus.CONFLICT),
    PIN_MISMATCH("ERR_654", HttpStatus.BAD_REQUEST),
    
    // Profile Update errors (700 series)
    PROFILE_UPDATE_FAILED("ERR_700", HttpStatus.INTERNAL_SERVER_ERROR),
    PROFILE_EMAIL_FORMAT("ERR_701", HttpStatus.BAD_REQUEST),
    PROFILE_PHONE_FORMAT("ERR_702", HttpStatus.BAD_REQUEST),
    PROFILE_DUPLICATE_EMAIL("ERR_703", HttpStatus.CONFLICT),
    PROFILE_DUPLICATE_PHONE("ERR_704", HttpStatus.CONFLICT),
    INVALID_DATE_FORMAT("ERR_705", HttpStatus.BAD_REQUEST),
    INVALID_GENDER_VALUE("ERR_706", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELDS("ERR_707", HttpStatus.BAD_REQUEST),

    // OTP related errors (800 series)
    OTP_EXPIRED("ERR_801", HttpStatus.BAD_REQUEST),
    OTP_INVALID("ERR_802", HttpStatus.BAD_REQUEST),
    OTP_MAX_ATTEMPTS("ERR_803", HttpStatus.TOO_MANY_REQUESTS),
    OTP_NOT_FOUND("ERR_804", HttpStatus.NOT_FOUND),
    OTP_RATE_LIMIT("ERR_805", HttpStatus.TOO_MANY_REQUESTS),
    OTP_VERIFICATION_FAILED("ERR_806", HttpStatus.BAD_REQUEST),
    OTP_ALREADY_VERIFIED("ERR_807", HttpStatus.BAD_REQUEST),
    OTP_GENERATION_FAILED("ERR_808", HttpStatus.INTERNAL_SERVER_ERROR),
    OTP_SMS_FAILED("ERR_809", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // Authentication errors
    LOGIN_FAILED("ERR_301", HttpStatus.UNAUTHORIZED),
    
    // Phone number related errors (700 series)
    PHONE_UPDATE_FAILED("ERR_701", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // Transaction related errors (900 series)
    TRANSACTION_FAILED("ERR_901", HttpStatus.INTERNAL_SERVER_ERROR),
    INSUFFICIENT_FUNDS("ERR_902", HttpStatus.BAD_REQUEST),
    INVALID_AMOUNT("ERR_903", HttpStatus.BAD_REQUEST),
    WALLET_NOT_FOUND("ERR_904", HttpStatus.NOT_FOUND),
    UNAUTHORIZED_WALLET_ACCESS("ERR_905", HttpStatus.FORBIDDEN),
    CURRENCY_MISMATCH("ERR_906", HttpStatus.BAD_REQUEST),
    TRANSACTION_LIMIT_EXCEEDED("ERR_907", HttpStatus.BAD_REQUEST),
    TRANSACTION_AMOUNT_TOO_SMALL("ERR_908", HttpStatus.BAD_REQUEST),
    DAILY_LIMIT_EXCEEDED("ERR_909", HttpStatus.BAD_REQUEST),
    MONTHLY_LIMIT_EXCEEDED("ERR_910", HttpStatus.BAD_REQUEST),
    ANNUAL_LIMIT_EXCEEDED("ERR_911", HttpStatus.BAD_REQUEST),
    TRANSACTION_TYPE_NOT_FOUND("ERR_912", HttpStatus.NOT_FOUND),
    TRANSACTION_STATUS_NOT_FOUND("ERR_913", HttpStatus.NOT_FOUND),
    FEE_TYPE_NOT_FOUND("ERR_914", HttpStatus.NOT_FOUND),
    FEE_CONFIGURATION_NOT_FOUND("ERR_915", HttpStatus.NOT_FOUND),
    TRANSACTION_LIMITS_NOT_FOUND("ERR_916", HttpStatus.NOT_FOUND),
    TRANSACTION_NOT_FOUND("ERR_917", HttpStatus.NOT_FOUND),
    WALLET_ACCESS_DENIED("ERR_905", HttpStatus.FORBIDDEN),
    
    // Non-wallet transfer errors (920 series)
    RECIPIENT_DETAILS_INVALID("ERR_921", HttpStatus.BAD_REQUEST),
    RECIPIENT_ID_INVALID("ERR_922", HttpStatus.BAD_REQUEST),
    RECIPIENT_PHONE_INVALID("ERR_923", HttpStatus.BAD_REQUEST),
    DISBURSEMENT_STAGE_NOT_FOUND("ERR_924", HttpStatus.NOT_FOUND),
    NON_WALLET_TRANSFER_FAILED("ERR_925", HttpStatus.INTERNAL_SERVER_ERROR),
    RECIPIENT_DETAILS_NOT_FOUND("ERR_926", HttpStatus.NOT_FOUND),
    INVALID_RELATIONSHIP("ERR_927", HttpStatus.BAD_REQUEST),
    
    // Notification related errors (950 series)
    NOTIFICATION_TYPE_NOT_FOUND("ERR_951", HttpStatus.NOT_FOUND),
    NOTIFICATION_CHANNEL_NOT_FOUND("ERR_952", HttpStatus.NOT_FOUND),
    NOTIFICATION_FAILED("ERR_953", HttpStatus.INTERNAL_SERVER_ERROR);
    
    final String code;
    final HttpStatus httpStatus;

    ErrorCode(String code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    ErrorCode(String code) {
        this.code = code;
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    ErrorCode(int code, String description) {
        this.code = "ERR_" + code;
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
} 
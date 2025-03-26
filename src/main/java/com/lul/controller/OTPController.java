package com.lul.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lul.constant.ErrorCode;
import com.lul.dto.OtpVerificationRequest;
import com.lul.exception.BaseException;
import com.lul.service.OtpService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import java.util.Map;



@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
public class OTPController {
    private final OtpService otpService;

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(
            @RequestHeader("Authorization") String token,
            @RequestBody OtpVerificationRequest request,
            HttpServletRequest httpRequest) {
        try {
            token = token.replace("Bearer ", "");
            return ResponseEntity.ok(otpService.verifyOtp(token, request.getOtpCode(), httpRequest));
        } catch (BaseException e) {
            return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(Map.of(
                    "status", "error",
                    "code", e.getErrorCode().getCode()
                ));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "code", ErrorCode.OTP_VERIFICATION_FAILED.getCode()
                ));
        }
    }
}

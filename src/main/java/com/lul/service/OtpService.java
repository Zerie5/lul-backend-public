package com.lul.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import com.google.common.util.concurrent.RateLimiter;
import com.lul.entity.OtpLog;
import com.lul.entity.User;
import com.lul.entity.VerificationLog;
import com.lul.repository.OtpLogRepository;
import com.lul.repository.UserRepository;
import com.lul.repository.VerificationLogRepository;
import com.lul.exception.NotFoundException;
import com.lul.exception.TooManyRequestsException;

import com.lul.constant.ErrorCode;
import com.lul.exception.BadRequestException;
import com.lul.exception.ServiceException;
import com.lul.exception.BaseException;
import com.lul.repository.OtpStatusRepository;


@Service
@Slf4j
@Transactional
public class OtpService {
    private final OtpLogRepository otpLogRepository;
    private final UserRepository userRepository;
    private final SmsService smsService;
    private final RateLimiter otpRateLimiter;
    private final VerificationLogRepository verificationLogRepository;
    private final JwtService jwtService;
    private final OtpStatusRepository otpStatusRepository;

    @Value("${otp.rate-limit.max-attempts}")
    private int maxAttempts = 3;

    @Value("${otp.rate-limit.duration-minutes}")
    private int durationMinutes = 60;

    public OtpService(OtpLogRepository otpLogRepository, 
                     UserRepository userRepository,
                     SmsService smsService,
                     VerificationLogRepository verificationLogRepository,
                     JwtService jwtService,
                     OtpStatusRepository otpStatusRepository) {
        this.otpLogRepository = otpLogRepository;
        this.userRepository = userRepository;
        this.smsService = smsService;
        this.verificationLogRepository = verificationLogRepository;
        this.jwtService = jwtService;
        this.otpStatusRepository = otpStatusRepository;
        this.otpRateLimiter = RateLimiter.create(maxAttempts / (durationMinutes * 60.0));
    }

    @Transactional
    public Map<String, Object> verifyOtp(String token, String otpCode, HttpServletRequest request) {
        try {
            String userIdStr = jwtService.extractUserId(token);
            Long userId = Long.parseLong(userIdStr);

            int recentFailedAttempts = verificationLogRepository.countFailedAttemptsSince(
                userId, 
                LocalDateTime.now().minusHours(1)
            );

            if (recentFailedAttempts >= maxAttempts || !otpRateLimiter.tryAcquire()) {
                logVerificationAttempt(userId, request.getRemoteAddr(), false);
                throw new TooManyRequestsException(ErrorCode.OTP_RATE_LIMIT.getCode());
            }

            OtpLog otpLog = otpLogRepository.findLatestPendingOtpByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.OTP_NOT_FOUND));

            otpLogRepository.incrementAttemptCount(otpLog.getId());

            if (otpLog.getAttemptsCount() >= otpLog.getMaxAttempts()) {
                otpLog.setStatus(otpStatusRepository.findById(3).orElseThrow());
                otpLogRepository.save(otpLog);
                logVerificationAttempt(userId, request.getRemoteAddr(), false);
                throw new TooManyRequestsException(ErrorCode.OTP_MAX_ATTEMPTS.getCode());
            }

            if (!otpCode.equals(otpLog.getOtpCode())) {
                logVerificationAttempt(userId, request.getRemoteAddr(), false);
                throw new BadRequestException(ErrorCode.OTP_INVALID);
            }

            User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
            
            user.setPhoneVerified(true);
            user.setRegisterStatus(3);
            userRepository.save(user);

            otpLog.setStatus(otpStatusRepository.findById(2).orElseThrow());
            otpLog.setVerifiedAt(ZonedDateTime.now());
            otpLogRepository.save(otpLog);

            logVerificationAttempt(userId, request.getRemoteAddr(), true);

            return Map.of(
                "status", "success",
                "message", "Phone number verified successfully",
                "registrationStatus", 3
            );
        } catch (Exception e) {
            log.error("OTP verification failed", e);
            if (e instanceof BaseException) {
                throw e;
            }
            throw new ServiceException(ErrorCode.OTP_VERIFICATION_FAILED);
        }
    }

    @Transactional
    private void logVerificationAttempt(Long userId, String ipAddress, boolean success) {
        VerificationLog log = new VerificationLog();
        log.setUserId(userId);
        log.setAttemptTime(LocalDateTime.now());
        log.setIpAddress(ipAddress);
        log.setSuccess(success);
        verificationLogRepository.save(log);
    }
} 
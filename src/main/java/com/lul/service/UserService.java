package com.lul.service;

import com.lul.dto.UserRegistrationRequest;
import com.lul.entity.User;
import com.lul.entity.UserProfile;
import com.lul.entity.UserAgreement;
import com.lul.repository.UserRepository;
import com.lul.repository.UserProfileRepository;
import com.lul.repository.UserAgreementRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lul.constant.ErrorCode;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;


import java.util.Map;

import com.lul.entity.UserToken;
import com.lul.repository.UserTokenRepository;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

import java.security.SecureRandom;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import com.lul.dto.ProfileUpdateRequest;
import com.lul.entity.AccessHistory;
import com.lul.entity.OtpLog;
import com.lul.repository.OtpLogRepository;
import java.time.ZonedDateTime;



import java.util.HashMap;

import com.lul.repository.OtpMethodRepository;
import com.lul.repository.OtpStatusRepository;
import org.springframework.beans.factory.annotation.Value;
import com.lul.exception.NotFoundException;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.lul.service.WalletService;




@Service
@Slf4j
@Transactional
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int MAX_ATTEMPTS = 10;
    
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserAgreementRepository userAgreementRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();
    private final EmailService emailService;
    private final UserTokenRepository userTokenRepository;
    private final JwtService jwtService;
    private final AccessHistoryService accessHistoryService;
    private final IpGeolocationService ipGeolocationService;
    private final OtpLogRepository otpLogRepository;
    private final SmsService smsService;
    private final OtpMethodRepository otpMethodRepository;
    private final OtpStatusRepository otpStatusRepository;
    private final DeviceFingerprintService deviceFingerprintService;
    private final WalletService walletService;

    @Value("${otp.length}")
    private int otpLength;

    @Value("${otp.expiry-minutes}")
    private int otpExpiryMinutes;

    @Autowired
    public UserService(UserRepository userRepository, 
                      UserProfileRepository userProfileRepository,
                      UserAgreementRepository userAgreementRepository,
                      PasswordEncoder passwordEncoder,
                      EmailService emailService,
                      UserTokenRepository userTokenRepository,
                      JwtService jwtService,
                      AccessHistoryService accessHistoryService,
                      IpGeolocationService ipGeolocationService,
                      OtpLogRepository otpLogRepository,
                      SmsService smsService,
                      OtpMethodRepository otpMethodRepository,
                      OtpStatusRepository otpStatusRepository,
                      DeviceFingerprintService deviceFingerprintService,
                      WalletService walletService) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userAgreementRepository = userAgreementRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.userTokenRepository = userTokenRepository;
        this.jwtService = jwtService;
        this.accessHistoryService = accessHistoryService;
        this.ipGeolocationService = ipGeolocationService;
        this.otpLogRepository = otpLogRepository;
        this.smsService = smsService;
        this.otpMethodRepository = otpMethodRepository;
        this.otpStatusRepository = otpStatusRepository;
        this.deviceFingerprintService = deviceFingerprintService;
        this.walletService = walletService;
    }

    @Transactional
    public Map<String, Object> registerUser(UserRegistrationRequest request) {
        try {
            // Check for duplicate username
            if (userRepository.existsByUsername(request.getUsername())) {
                logger.warn("Registration failed: Username '{}' already exists", request.getUsername());
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("code", ErrorCode.DUPLICATE_USERNAME.getCode());
                response.put("message", "Username already exists");
                return response;
            }
            
            // Check for duplicate email
            if (userRepository.existsByEmail(request.getEmail())) {
                logger.warn("Registration failed: Email '{}' already exists", request.getEmail());
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("code", ErrorCode.DUPLICATE_EMAIL.getCode());
                response.put("message", "Email already exists");
                return response;
            }
            
            // Check for duplicate phone number
          /*   if (request.getPhoneNumber() != null && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                logger.warn("Registration failed: Phone number '{}' already exists", request.getPhoneNumber());
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("code", ErrorCode.DUPLICATE_PHONE.getCode());
                response.put("message", "Phone number already exists");
                return response;
            }
            */
            
            User user = new User();
            user.setEmail(request.getEmail());
            user.setUsername(request.getUsername());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setRegisterStatus(2);  // Pending OTP
            user.setStatusId(1);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            String userWorkId = generateUniqueUserWorkId();
            user.setUserWorkId(userWorkId);
            
            user = userRepository.save(user);

            // Create user profile with ALL fields from ProfileBase
            UserProfile profile = new UserProfile();
            profile.setUser(user);
            profile.setFirstName(request.getFirstName());
            profile.setLastName(request.getLastName());
            profile.setWhatsappNumber(request.getWhatsappNumber());
            profile.setGender(request.getGender());
            profile.setDateOfBirth(request.getDateOfBirth());
            profile.setCity(request.getCity());
            profile.setCountry(request.getCountry());
            profile.setState(request.getState());
            profile.setKycLevel(1);
            profile.setReferredBy(request.getReferredBy());
            userProfileRepository.save(profile);

            // Create user agreement with only required fields
            UserAgreement agreement = new UserAgreement();
            agreement.setUser(user);
            agreement.setTermsId(1L);
            agreement.setAgreedAt(LocalDateTime.now());
            userAgreementRepository.save(agreement);

            // Create OTP record
            OtpLog otpLog = new OtpLog();
            otpLog.setUserId(user.getId());
            otpLog.setOtpCode(generateOtpCode());
            otpLog.setStatus(otpStatusRepository.findById(1).orElseThrow());
            otpLog.setMethod(otpMethodRepository.findById(1).orElseThrow());
            otpLog.setSentAt(ZonedDateTime.now());
            otpLog.setMaxAttempts(3);
            otpLog.setAttemptsCount(0);
            otpLogRepository.save(otpLog);

            // Send OTP via SMS
            boolean smsSent = smsService.sendSms(
                user.getPhoneNumber(),
                String.format("Your LulPay verification code is: %s", otpLog.getOtpCode())
            );
            
            if (!smsSent) {
                log.warn("Failed to send SMS OTP to user: {}", user.getPhoneNumber());
            }

            // Create user token
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", user.getId().toString());
            claims.put("deviceId", request.getDeviceInfo().getDeviceId());
            claims.put("type", "REGISTRATION");
            
            String token = jwtService.generateToken(claims);
            
            UserToken userToken = new UserToken();
            userToken.setUser(user);
            userToken.setToken(token);
            userToken.setDeviceId(request.getDeviceInfo().getDeviceId());
            userToken.setDeviceName(request.getDeviceInfo().getDeviceName());
            userToken.setDeviceFingerprint(deviceFingerprintService.generateFingerprint(request.getDeviceInfo()));
            userToken.setCreatedAt(LocalDateTime.now());
            userToken.setFirstSeenAt(LocalDateTime.now());
            userToken.setExpiresAt(LocalDateTime.now().plusMonths(6));
            userTokenRepository.save(userToken);

            // Record access history
            AccessHistory accessHistory = new AccessHistory();
            accessHistory.setUserId(user.getId());
            accessHistory.setDeviceName(request.getDeviceInfo().getDeviceName());
            accessHistory.setDeviceId(request.getDeviceInfo().getDeviceId());
            accessHistory.setOs(request.getDeviceInfo().getOs());
            accessHistory.setIpAddress(getClientIpAddress());
            accessHistory.setAccessTime(LocalDateTime.now());
            accessHistory.setCurrentSession(true);
            accessHistoryService.save(accessHistory);

            // Generate response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("token", userToken.getToken());
            response.put("userId", user.getUserWorkId());
            response.put("registerStatus", user.getRegisterStatus());
            
            return response;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            logger.error("Database constraint violation during registration", e);
            
            // Check for specific constraint violations
            String errorMessage = e.getMessage().toLowerCase();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            
            if (errorMessage.contains("users_username_key") || errorMessage.contains("username")) {
                response.put("code", ErrorCode.DUPLICATE_USERNAME.getCode());
                response.put("message", "Username already exists");
            } else if (errorMessage.contains("users_email_key") || errorMessage.contains("email")) {
                response.put("code", ErrorCode.DUPLICATE_EMAIL.getCode());
                response.put("message", "Email already exists");
            } else if (errorMessage.contains("users_phone_number_key") || errorMessage.contains("phone")) {
                response.put("code", ErrorCode.DUPLICATE_PHONE.getCode());
                response.put("message", "Phone number already exists");
            } else {
                response.put("code", ErrorCode.REGISTRATION_FAILED.getCode());
                response.put("message", "Registration failed due to a database constraint violation");
            }
            
            return response;
        } catch (Exception e) {
            logger.error("Error during registration", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", ErrorCode.REGISTRATION_FAILED.getCode());
            response.put("message", "Registration failed due to an unexpected error");
            
            return response;
        }
    }

    private String generateUserWorkId() {
        SecureRandom secureRandom = new SecureRandom();
        
        // Available letters (excluding O, I for clarity)
        char[] availableLetters = "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();  // 24 letters
        
        // Generate 2 random letters
        char letter1 = availableLetters[secureRandom.nextInt(availableLetters.length)];
        char letter2 = availableLetters[secureRandom.nextInt(availableLetters.length)];
        
        // Get current year and month
        LocalDateTime now = LocalDateTime.now();
        String year = String.format("%02d", now.getYear() % 100);
        String month = String.format("%02d", now.getMonthValue());
        
        // Generate 4-digit random number
        String sequence = String.format("%04d", secureRandom.nextInt(10000));
        
        return String.format("%c%c%s%s%s", letter1, letter2, year, month, sequence);
    }

    private String generateUniqueUserWorkId() {
        String userWorkId;
        int attempts = 0;
        
        do {
            if (attempts >= MAX_ATTEMPTS) {
                throw new RuntimeException("Failed to generate unique user work ID after " + MAX_ATTEMPTS + " attempts");
            }
            userWorkId = generateUserWorkId();
            attempts++;
        } while (userRepository.existsByUserWorkId(userWorkId));
        
        return userWorkId;
    }

    public Optional<User> getUserProfile(String email) {
        logger.debug("Fetching user profile for email: {}", email);
        return userRepository.findByEmail(email);
    }

    public ResponseEntity<?> updateProfile(Long userId, ProfileUpdateRequest request) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Update user profile with request data
            UserProfile profile = user.getProfile();
            if (profile == null) {
                profile = new UserProfile();
                profile.setUser(user);
            }
            
            // Update fields
            profile.setFirstName(request.getFirstName());
            profile.setLastName(request.getLastName());
            profile.setWhatsappNumber(request.getWhatsappNumber());
            profile.setCity(request.getCity());
            profile.setState(request.getState());
            profile.setCountry(request.getCountry());
            profile.setGender(request.getGender());
            profile.setDateOfBirth(request.getDateOfBirth());
            
            userProfileRepository.save(profile);
            
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "code", "ERR_503"));
        }
    }

    private String generateOtpCode() {
        int otp = 100000 + secureRandom.nextInt(900000); // Generates number between 100000 and 999999
        return String.format("%06d", otp);
    }

    @Transactional
    public Map<String, Object> updatePhoneNumber(String token, String newPhoneNumber) {
        try {
            token = token.replace("Bearer ", "");
            String userIdStr = jwtService.extractUserId(token);
            Long userId = Long.parseLong(userIdStr);
            
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
                
            // Format phone number (remove '+' if present)
            newPhoneNumber = newPhoneNumber.startsWith("+") ? 
                newPhoneNumber.substring(1) : newPhoneNumber;
            
            user.setPhoneNumber(newPhoneNumber);
            user.setPhoneVerified(false);  // Reset verification status
            userRepository.save(user);
            
            // Generate new OTP for verification
            String otpCode = generateOtpCode();
            OtpLog otpLog = new OtpLog();
            otpLog.setUserId(userId);
            otpLog.setOtpCode(otpCode);
            otpLog.setStatus(otpStatusRepository.findById(1).orElseThrow());
            otpLog.setMethod(otpMethodRepository.findById(1).orElseThrow());
            otpLog.setSentAt(ZonedDateTime.now());
            otpLog.setMaxAttempts(3);
            otpLog.setAttemptsCount(0);
            otpLogRepository.save(otpLog);
            
            // Send OTP via SMS
            boolean smsSent = smsService.sendSms(
                newPhoneNumber,
                String.format("Your LulPay verification code is: %s", otpLog.getOtpCode())
            );
            
            if (!smsSent) {
                log.warn("Failed to send SMS OTP to new phone number: {}", newPhoneNumber);
            }
            
            return Map.of(
                "status", "success",
                "message", "Phone number updated. Please verify with OTP."
            );
        } catch (Exception e) {
            logger.error("Phone number update failed: ", e);
            throw e;
        }
    }

    @Transactional
    public Map<String, Object> createPin(String token, String pin) {
        String userIdStr = jwtService.extractUserId(token);
        Long userId = Long.parseLong(userIdStr);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
            
        user.setRegisterStatus(4); // USER_ACTIVATED
        userRepository.save(user);
        
        // Create default wallets for the user
        Map<String, Object> walletResult = walletService.createDefaultWallets(user);
        
        // Add wallet information to the response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("wallets", walletResult.get("walletIds"));
        
        return response;
    }

    private String getClientIpAddress() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
                
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Retrieves user information by their work ID
     * 
     * @param userWorkId The 10-character user work ID
     * @return Map containing user information (firstName, lastName, workId, email, phoneNumber)
     * @throws NotFoundException if user with the given work ID is not found
     */
    public Map<String, Object> getUserByWorkId(String userWorkId) {
        User user = userRepository.findByUserWorkId(userWorkId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", user.getFirstName());
        userData.put("lastName", user.getLastName());
        userData.put("workId", user.getUserWorkId());
        userData.put("email", user.getEmail());
        userData.put("phoneNumber", user.getPhoneNumber());
        
        return userData;
    }
} 
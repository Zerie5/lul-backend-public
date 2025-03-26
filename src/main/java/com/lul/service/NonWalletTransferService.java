package com.lul.service;

import com.lul.constant.ErrorCode;
import com.lul.dto.NonWalletTransferRequest;
import com.lul.dto.NonWalletTransferResponse;
import com.lul.entity.*;
import com.lul.exception.InsufficientFundsException;
import com.lul.exception.InvalidPinException;
import com.lul.exception.LulPayException;
import com.lul.exception.NotFoundException;
import com.lul.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for handling transfers to non-wallet recipients
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NonWalletTransferService {

    private final UserWalletRepository userWalletRepository;
    private final UserRepository userRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final PinService pinService;
    private final NotificationService notificationService;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final TransactionFeeRepository transactionFeeRepository;
    private final FeeTypeRepository feeTypeRepository;
    private final TransactionService transactionService;
    private final TransactionAuditLogRepository transactionAuditLogRepository;
    private final NotificationProcessor notificationProcessor;
    private final NonWalletRecipientDetailRepository nonWalletRecipientDetailRepository;
    private final DisbursementStageRepository disbursementStageRepository;
    private final SmsService smsService;
    
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Generate a unique transaction ID using the database sequence
     * 
     * @return The generated transaction ID
     */
    private Long generateTransactionId() {
        return (Long) entityManager.createNativeQuery("SELECT nextval('transaction_id_seq')").getSingleResult();
    }

    /**
     * Transfer funds to a non-wallet recipient
     * 
     * @param senderId The ID of the sender user
     * @param request The transfer request containing all details
     * @return The transfer response
     */
    @Transactional
    public NonWalletTransferResponse transferToNonWallet(Long senderId, NonWalletTransferRequest request) {
        log.info("Processing non-wallet transfer: sender={}, walletType={}, amount={}",
                senderId, request.getSenderWalletTypeId(), request.getAmount());
        
        try {
            // Check idempotency if key is provided
            if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isEmpty()) {
                Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findByKey(request.getIdempotencyKey());
                if (existingKey.isPresent()) {
                    log.info("Idempotency key found, returning existing transaction: {}", request.getIdempotencyKey());
                    // Return existing transaction details
                    TransactionHistory existingTransaction = transactionHistoryRepository.findByTransactionId(existingKey.get().getTransactionId())
                        .orElseThrow(() -> new NotFoundException(ErrorCode.TRANSACTION_NOT_FOUND));
                    
                    UserWallet senderWallet = userWalletRepository.findById(Integer.valueOf(existingTransaction.getSenderWalletId()))
                        .orElseThrow(() -> new NotFoundException(ErrorCode.WALLET_NOT_FOUND));
                    
                    // Get disbursement stage name
                    String disbursementStageName = "Processing"; // Default
                    try {
                        DisbursementStage stage = disbursementStageRepository.findById(existingTransaction.getDisbursementStageId())
                            .orElse(null);
                        if (stage != null) {
                            disbursementStageName = stage.getStageName();
                        }
                    } catch (Exception e) {
                        log.error("Error fetching disbursement stage: {}", e.getMessage(), e);
                    }
                    
                    // Find recipient details
                    NonWalletRecipientDetail recipientDetail = nonWalletRecipientDetailRepository
                        .findByTransactionHistoryId(existingTransaction.getId())
                        .orElse(null);
                    
                    String recipientName = recipientDetail != null ? recipientDetail.getFullName() : "Unknown";
                    String recipientPhone = recipientDetail != null ? recipientDetail.getPhoneNumber() : "Unknown";
                    
                    return NonWalletTransferResponse.builder()
                        .status("success")
                        .transactionId(existingTransaction.getTransactionId())
                        .senderWalletId(Long.valueOf(senderWallet.getId()))
                        .amount(existingTransaction.getTransactedValue())
                        .fee(existingTransaction.getFee())
                        .totalAmount(existingTransaction.getTotalAmount())
                        .currency(existingTransaction.getCurrency())
                        .description(existingTransaction.getDescription())
                        .timestamp(existingTransaction.getCreatedAt())
                        .senderWalletBalanceAfter(senderWallet.getBalance())
                        .recipientName(recipientName)
                        .recipientPhoneNumber(recipientPhone)
                        .disbursementStageId(existingTransaction.getDisbursementStageId())
                        .disbursementStageName(disbursementStageName)
                        .build();
                }
            }
            
            // 1. Find the sender's wallet by user ID and wallet type ID
            UserWallet senderWallet = userWalletRepository.findByUserIdAndWalletId(senderId, request.getSenderWalletTypeId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.WALLET_NOT_FOUND));
            
            // 2. Find the sender user
            User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
            
            // 3. Find the company wallet with the same currency
            UserWallet companyWallet;
            if (request.getSenderWalletTypeId() == 1) { // UGX
                companyWallet = userWalletRepository.findById(Integer.valueOf(7)) // Company UGX wallet
                    .orElseThrow(() -> new LulPayException(ErrorCode.WALLET_NOT_FOUND, "Company UGX wallet not found"));
            } else if (request.getSenderWalletTypeId() == 2) { // USD
                companyWallet = userWalletRepository.findById(Integer.valueOf(6)) // Company USD wallet
                    .orElseThrow(() -> new LulPayException(ErrorCode.WALLET_NOT_FOUND, "Company USD wallet not found"));
            } else {
                throw new LulPayException(ErrorCode.INVALID_AMOUNT, "Wallet type not supported");
            }
            
            // 4. Verify PIN
            if (!pinService.verifyPin(sender, request.getPin())) {
                throw new InvalidPinException(ErrorCode.INVALID_PIN);
            }
            
            // 5. Check if sender has sufficient funds
            if (senderWallet.getBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientFundsException(ErrorCode.INSUFFICIENT_FUNDS);
            }
            
            // 6. Calculate fees
            BigDecimal fee = calculateTransactionFee(request.getAmount(), request.getSenderWalletTypeId());
            BigDecimal totalAmount = request.getAmount().add(fee);
            
            if (senderWallet.getBalance().compareTo(totalAmount) < 0) {
                throw new InsufficientFundsException(ErrorCode.INSUFFICIENT_FUNDS);
            }
            
            // 7. Create transaction record
            TransactionHistory transaction = new TransactionHistory();
            transaction.setSenderId(senderId.intValue());
            transaction.setReceiverWalletId(companyWallet.getId().intValue());
            transaction.setSenderWalletId(senderWallet.getId().intValue());
            transaction.setTransactedValue(request.getAmount());
            transaction.setFee(fee);
            transaction.setTotalAmount(totalAmount);
            transaction.setCurrency(senderWallet.getWallet().getCurrencyCode());
            transaction.setDescription(request.getDescription());
            transaction.setTransactionTypeId(3); // Assuming 3 is for non-wallet transfer
            transaction.setTransactionStatusId(1); // Initiated
            transaction.setCreatedAt(LocalDateTime.now());
            transaction.setDisbursementStageId(2); // Processing stage
            
            // Generate a unique transaction ID using the database sequence
            long transactionId = generateTransactionId();
            transaction.setTransactionId(transactionId);
            
            // Set additional data as a Map
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("senderName", sender.getFirstName() + " " + sender.getLastName());
            additionalData.put("recipientName", request.getRecipientFullName());
            additionalData.put("recipientPhone", request.getPhoneNumber());
            additionalData.put("transferMethod", "non_wallet");
            transaction.setAdditionalData(additionalData);
            
            transactionHistoryRepository.save(transaction);
            
            // 8. Create non-wallet recipient details
            NonWalletRecipientDetail recipientDetail = NonWalletRecipientDetail.builder()
                .transactionHistoryId(Long.valueOf(transaction.getId()))
                .fullName(request.getRecipientFullName())
                .idDocumentType(request.getIdDocumentType())
                .idNumber(request.getIdNumber())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .country(request.getCountry())
                .state(request.getState())
                .city(request.getCity())
                .relationship(request.getRelationship())
                .disbursementStageId(2) // Processing stage
                .build();
            
            nonWalletRecipientDetailRepository.save(recipientDetail);
            
            // 9. Update wallet balances
            senderWallet.setBalance(senderWallet.getBalance().subtract(totalAmount));
            companyWallet.setBalance(companyWallet.getBalance().add(request.getAmount()));
            
            userWalletRepository.save(senderWallet);
            userWalletRepository.save(companyWallet);
            
            // 10. Update transaction status to completed
            transaction.setTransactionStatusId(2); // Assuming 2 is for completed
            transaction.setCompletedAt(LocalDateTime.now());
            transactionHistoryRepository.save(transaction);
            
            // 11. Record transaction fee
            recordTransactionFee(transaction.getTransactionId(), fee, senderWallet.getWallet().getCurrencyCode());
            
            // 12. Create audit log
            createAuditLog(transaction, senderId.intValue(), "NON_WALLET_TRANSFER_COMPLETED", null);
            
            // 13. Send notifications to sender (FCM and SMS)
            String senderFirstName = sender.getFirstName() != null ? sender.getFirstName() : "User";
            String recipientFirstName = request.getRecipientFullName() != null ? request.getRecipientFullName() : "Recipient";
            String currencyCode = senderWallet.getWallet().getCurrencyCode();
            String transactionIdStr = transaction.getTransactionId().toString();
            String currentDate = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            
            // FCM message for sender
            String fcmSenderMessage = "You Sent " + request.getAmount() + " " + currencyCode + " to " + recipientFirstName;
            
            // SMS message for sender
            String smsSenderMessage = "Hello " + senderFirstName + ", you sent " + request.getAmount() + " " + currencyCode + 
                " to " + recipientFirstName + " at " + currentDate + ". Transaction id is " + transactionIdStr;
            
            // Send FCM notification to sender (but not SMS - we'll send that directly)
            notificationService.sendFcmNotificationToSender(
                senderId.intValue(),
                "Payment Sent",
                fcmSenderMessage,
                transaction.getTransactionId()
            );
            
            // Also send SMS directly to sender to ensure delivery
            try {
                // Ensure sender phone number is properly formatted
                String senderPhoneNumber = sender.getPhoneNumber();
                if (senderPhoneNumber != null && !senderPhoneNumber.isEmpty()) {
                    log.info("Sending direct SMS to sender phone: {}", senderPhoneNumber);
                    
                    // Ensure phone number is properly formatted
                    if (!senderPhoneNumber.startsWith("+")) {
                        if (senderPhoneNumber.startsWith("0")) {
                            senderPhoneNumber = "+256" + senderPhoneNumber.substring(1);
                        } else {
                            senderPhoneNumber = "+" + senderPhoneNumber;
                        }
                        log.info("Formatted sender phone number: {}", senderPhoneNumber);
                    }
                    
                    boolean smsSent = notificationService.sendDirectSms(senderPhoneNumber, smsSenderMessage);
                    if (smsSent) {
                        log.info("SMS sent directly to sender: {}", senderPhoneNumber);
                    } else {
                        log.warn("SMS service failed to send message directly to sender: {}", senderPhoneNumber);
                    }
                } else {
                    log.warn("Sender phone number is null or empty. Cannot send SMS.");
                }
            } catch (Exception ex) {
                log.error("Failed to send SMS directly to sender: {}", ex.getMessage(), ex);
                // Log the full stack trace for better troubleshooting
                ex.printStackTrace();
                // Don't fail the transaction if SMS fails
            }
            
            // 14. Send SMS to recipient
            // SMS message for recipient
            String smsReceiverMessage = "Hello " + recipientFirstName + ", you received " + request.getAmount() + " " + currencyCode + 
                " from " + senderFirstName + " at " + currentDate + ". Transaction id is " + transactionIdStr;
            
            log.info("Sending SMS to recipient: {}", smsReceiverMessage);
            
            // Send SMS directly to non-wallet recipient instead of using notification queue
            try {
                // Ensure recipient phone number is properly formatted
                String recipientPhoneNumber = request.getPhoneNumber();
                if (recipientPhoneNumber != null && !recipientPhoneNumber.isEmpty()) {
                    log.info("Sending direct SMS to recipient phone: {}", recipientPhoneNumber);
                    
                    // Ensure phone number is properly formatted
                    if (!recipientPhoneNumber.startsWith("+")) {
                        if (recipientPhoneNumber.startsWith("0")) {
                            recipientPhoneNumber = "+256" + recipientPhoneNumber.substring(1);
                        } else {
                            recipientPhoneNumber = "+" + recipientPhoneNumber;
                        }
                        log.info("Formatted recipient phone number: {}", recipientPhoneNumber);
                    }
                    
                    boolean smsSent = notificationService.sendDirectSms(recipientPhoneNumber, smsReceiverMessage);
                    if (smsSent) {
                        log.info("SMS sent directly to recipient: {}", recipientPhoneNumber);
                    } else {
                        log.warn("SMS service failed to send message directly to recipient: {}", recipientPhoneNumber);
                    }
                } else {
                    log.warn("Recipient phone number is null or empty. Cannot send SMS.");
                }
            } catch (Exception ex) {
                log.error("Failed to send SMS directly to recipient: {}", ex.getMessage(), ex);
                // Log the full stack trace for better troubleshooting
                ex.printStackTrace();
                // Don't fail the transaction if SMS fails
            }
            
            // 15. Create idempotency key if provided
            if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isEmpty()) {
                IdempotencyKey newKey = new IdempotencyKey();
                newKey.setKey(request.getIdempotencyKey());
                newKey.setTransactionId(transaction.getTransactionId());
                newKey.setExpiresAt(LocalDateTime.now().plusDays(1));
                idempotencyKeyRepository.save(newKey);
            }
            
            // Process all pending notifications immediately to ensure they're sent right away
            notificationProcessor.processAllPendingNotificationsNow();
            
            // Get disbursement stage name
            String disbursementStageName = "Processing"; // Default
            try {
                DisbursementStage stage = disbursementStageRepository.findById(2)
                    .orElse(null);
                if (stage != null) {
                    disbursementStageName = stage.getStageName();
                }
            } catch (Exception e) {
                log.error("Error fetching disbursement stage: {}", e.getMessage(), e);
            }
            
            // 16. Build and return response
            return NonWalletTransferResponse.builder()
                .status("success")
                .transactionId(transaction.getTransactionId())
                .senderWalletId(Long.valueOf(senderWallet.getId()))
                .amount(request.getAmount())
                .fee(fee)
                .totalAmount(totalAmount)
                .currency(senderWallet.getWallet().getCurrencyCode())
                .description(request.getDescription())
                .timestamp(transaction.getCreatedAt())
                .senderWalletBalanceAfter(senderWallet.getBalance())
                .recipientName(request.getRecipientFullName())
                .recipientPhoneNumber(request.getPhoneNumber())
                .disbursementStageId(2)
                .disbursementStageName(disbursementStageName)
                .build();
                
        } catch (Exception e) {
            log.error("Non-wallet transfer failed: {}", e.getMessage(), e);
            if (e instanceof LulPayException || e instanceof InsufficientFundsException || e instanceof InvalidPinException || e instanceof NotFoundException) {
                throw e;
            }
            throw new LulPayException(ErrorCode.NON_WALLET_TRANSFER_FAILED, "Non-wallet transfer failed: " + e.getMessage());
        }
    }
    
    /**
     * Calculate the transaction fee for a non-wallet transfer
     * 
     * @param amount The amount being transferred
     * @param walletTypeId The wallet type ID
     * @return The calculated fee
     */
    private BigDecimal calculateTransactionFee(BigDecimal amount, Integer walletTypeId) {
        // Fetch fee configuration from database
        // For now, using a simple percentage-based fee
        BigDecimal feePercentage = BigDecimal.valueOf(0.01); // 1%
        BigDecimal fee = amount.multiply(feePercentage).setScale(2, RoundingMode.HALF_UP);
        
        // Ensure minimum fee
        BigDecimal minFee = walletTypeId == 1 ? BigDecimal.valueOf(500) : BigDecimal.valueOf(0.5); // 500 UGX or 0.5 USD
        return fee.max(minFee);
    }
    
    /**
     * Record the transaction fee
     * 
     * @param transactionId The transaction ID
     * @param feeAmount The fee amount
     * @param currency The currency
     */
    private void recordTransactionFee(Long transactionId, BigDecimal feeAmount, String currency) {
        try {
            // Use REMITTANCE_FEE (ID 4) for non-wallet transfers
            FeeType feeType = feeTypeRepository.findById(4)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FEE_TYPE_NOT_FOUND));
            
            TransactionFee transactionFee = new TransactionFee();
            transactionFee.setTransactionId(transactionId);
            transactionFee.setFeeTypeId(feeType.getId());
            transactionFee.setAmount(feeAmount);
            transactionFee.setCurrency(currency);
            transactionFee.setCreatedAt(LocalDateTime.now());
            
            transactionFeeRepository.save(transactionFee);
            log.info("Recorded transaction fee: transactionId={}, amount={}, currency={}", 
                    transactionId, feeAmount, currency);
        } catch (Exception e) {
            log.error("Failed to record transaction fee: {}", e.getMessage(), e);
            // Don't fail the transaction if fee recording fails
        }
    }
    
    /**
     * Create an audit log entry for the transaction
     * 
     * @param transaction The transaction
     * @param performedBy The user ID who performed the action
     * @param action The action performed
     * @param ipAddress The IP address (optional)
     */
    private void createAuditLog(TransactionHistory transaction, Integer performedBy, String action, String ipAddress) {
        try {
            TransactionAuditLog auditLog = new TransactionAuditLog();
            auditLog.setTransactionId(transaction.getTransactionId());
            auditLog.setPerformedBy(performedBy);
            auditLog.setAction(action);
            auditLog.setIpAddress(ipAddress);
            auditLog.setCreatedAt(LocalDateTime.now());
            
            // Create a JSON string for details
            Map<String, Object> details = new HashMap<>();
            details.put("transactionAmount", transaction.getTransactedValue());
            details.put("fee", transaction.getFee());
            details.put("totalAmount", transaction.getTotalAmount());
            details.put("currency", transaction.getCurrency());
            details.put("senderWalletId", transaction.getSenderWalletId());
            details.put("receiverWalletId", transaction.getReceiverWalletId());
            details.put("transactionType", "NON_WALLET_TRANSFER");
            
            // Convert details to JSON string if needed
            // auditLog.setDetails(objectMapper.writeValueAsString(details));
            
            transactionAuditLogRepository.save(auditLog);
            log.info("Created audit log: transactionId={}, action={}", transaction.getTransactionId(), action);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage(), e);
            // Don't fail the transaction if audit logging fails
        }
    }

    /**
     * Get the status of a non-wallet transfer by transaction ID
     * 
     * @param userId The ID of the user requesting the status
     * @param transactionId The ID of the transaction
     * @return The transfer response with current status
     */
    public NonWalletTransferResponse getNonWalletTransferStatus(Long userId, Long transactionId) {
        log.info("Getting non-wallet transfer status: userId={}, transactionId={}", userId, transactionId);
        
        // Find the transaction
        TransactionHistory transaction = transactionHistoryRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.TRANSACTION_NOT_FOUND));
        
        // Verify that the user is the sender of the transaction
        if (!transaction.getSenderId().equals(userId.intValue())) {
            throw new LulPayException(ErrorCode.UNAUTHORIZED_WALLET_ACCESS, "User is not authorized to view this transaction");
        }
        
        // Find the sender's wallet
        UserWallet senderWallet = userWalletRepository.findById(transaction.getSenderWalletId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.WALLET_NOT_FOUND));
        
        // Find recipient details
        NonWalletRecipientDetail recipientDetail = nonWalletRecipientDetailRepository
            .findByTransactionHistoryId(transaction.getId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.RECIPIENT_DETAILS_NOT_FOUND));
        
        // Get disbursement stage name
        String disbursementStageName = "Unknown";
        try {
            DisbursementStage stage = disbursementStageRepository.findById(transaction.getDisbursementStageId())
                .orElse(null);
            if (stage != null) {
                disbursementStageName = stage.getStageName();
            }
        } catch (Exception e) {
            log.error("Error fetching disbursement stage: {}", e.getMessage(), e);
        }
        
        // Build and return response
        return NonWalletTransferResponse.builder()
            .status(getStatusText(transaction.getTransactionStatusId()))
            .transactionId(transaction.getTransactionId())
            .senderWalletId(Long.valueOf(senderWallet.getId()))
            .amount(transaction.getTransactedValue())
            .fee(transaction.getFee())
            .totalAmount(transaction.getTotalAmount())
            .currency(transaction.getCurrency())
            .description(transaction.getDescription())
            .timestamp(transaction.getCreatedAt())
            .senderWalletBalanceAfter(senderWallet.getBalance())
            .recipientName(recipientDetail.getFullName())
            .recipientPhoneNumber(recipientDetail.getPhoneNumber())
            .disbursementStageId(transaction.getDisbursementStageId())
            .disbursementStageName(disbursementStageName)
            .build();
    }
    
    /**
     * Convert transaction status ID to a human-readable status text
     * 
     * @param statusId The transaction status ID
     * @return The human-readable status text
     */
    private String getStatusText(Integer statusId) {
        switch (statusId) {
            case 1:
                return "pending";
            case 2:
                return "completed";
            case 3:
                return "failed";
            case 4:
                return "cancelled";
            default:
                return "unknown";
        }
    }
} 
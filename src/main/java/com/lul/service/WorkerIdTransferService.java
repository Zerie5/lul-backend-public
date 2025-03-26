package com.lul.service;

import com.lul.constant.ErrorCode;
import com.lul.dto.WalletTransferResponse;
import com.lul.entity.*;
import com.lul.exception.InsufficientFundsException;
import com.lul.exception.InvalidPinException;
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
 * Service for handling wallet-to-wallet transfers using worker ID
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerIdTransferService {

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
     * Transfer funds between wallets using worker ID for the receiver
     * 
     * @param senderId The ID of the sender user
     * @param senderWalletTypeId The wallet type ID (currency) of the sender
     * @param receiverWorkerId The worker ID of the receiver
     * @param amount The amount to transfer
     * @param pin The sender's PIN for authorization
     * @param description Optional description of the transfer
     * @param idempotencyKey Optional key to prevent duplicate transfers
     * @return The transfer response
     */
    @Transactional
    public WalletTransferResponse transferByWorkerId(
            Long senderId, 
            Integer senderWalletTypeId,
            String receiverWorkerId,
            BigDecimal amount,
            String pin,
            String description,
            String idempotencyKey) {
        
        log.info("Processing wallet transfer by worker ID: sender={}, walletType={}, receiver={}, amount={}",
                senderId, senderWalletTypeId, receiverWorkerId, amount);
        
        try {
            // Check idempotency if key is provided
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findByKey(idempotencyKey);
                if (existingKey.isPresent()) {
                    log.info("Idempotency key found, returning existing transaction: {}", idempotencyKey);
                    // Return existing transaction details
                    // Implementation depends on how you store transaction results
                    throw new UnsupportedOperationException("Idempotency handling not fully implemented");
                }
            }
            
            // 1. Find the sender's wallet by user ID and wallet type ID
            UserWallet senderWallet = userWalletRepository.findByUserIdAndWalletId(senderId, senderWalletTypeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WALLET_NOT_FOUND));
            
            // 2. Find the receiver by worker ID
            User receiver = userRepository.findByUserWorkId(receiverWorkerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
            
            // 3. Find the receiver's wallet with the same wallet type ID
            UserWallet receiverWallet = userWalletRepository.findByUserIdAndWalletId(receiver.getId(), senderWalletTypeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WALLET_NOT_FOUND));
            
            // 4. Verify PIN
            User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
            
            if (!pinService.verifyPin(sender, pin)) {
                throw new InvalidPinException(ErrorCode.INVALID_PIN);
            }
            
            // 5. Check if sender has sufficient funds
            if (senderWallet.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException(ErrorCode.INSUFFICIENT_FUNDS);
            }
            
            // 6. Check transaction limits
            // Implementation depends on your limit checking logic
            
            // 7. Calculate fees
            BigDecimal fee = calculateTransactionFee(amount, senderWalletTypeId);
            BigDecimal totalAmount = amount.add(fee);
            
            if (senderWallet.getBalance().compareTo(totalAmount) < 0) {
                throw new InsufficientFundsException(ErrorCode.INSUFFICIENT_FUNDS);
            }
            
            // 8. Create transaction record
            TransactionHistory transaction = new TransactionHistory();
            transaction.setSenderId(senderId.intValue());
            transaction.setReceiverId(receiver.getId().intValue());
            transaction.setSenderWalletId(senderWallet.getId().intValue());
            transaction.setReceiverWalletId(receiverWallet.getId().intValue());
            transaction.setTransactedValue(amount);
            transaction.setFee(fee);
            transaction.setTotalAmount(totalAmount);
            transaction.setCurrency(senderWallet.getWallet().getCurrencyCode());
            transaction.setDescription(description);
            transaction.setTransactionTypeId(1); // Assuming 1 is for wallet-to-wallet transfer
            transaction.setTransactionStatusId(1); // Assuming 1 is for pending
            transaction.setCreatedAt(LocalDateTime.now());
            
            // Generate a unique transaction ID using the database sequence
            long transactionId = generateTransactionId();
            transaction.setTransactionId(transactionId);
            
            // Set additional data as a Map
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("senderName", sender.getFirstName() + " " + sender.getLastName());
            additionalData.put("receiverName", receiver.getFirstName() + " " + receiver.getLastName());
            additionalData.put("receiverWorkerId", receiverWorkerId);
            additionalData.put("transferMethod", "worker_id");
            transaction.setAdditionalData(additionalData);
            
            transactionHistoryRepository.save(transaction);
            
            // 9. Update wallet balances
            senderWallet.setBalance(senderWallet.getBalance().subtract(totalAmount));
            receiverWallet.setBalance(receiverWallet.getBalance().add(amount));
            
            userWalletRepository.save(senderWallet);
            userWalletRepository.save(receiverWallet);
            
            // 10. Update transaction status to completed
            transaction.setTransactionStatusId(2); // Assuming 2 is for completed
            transaction.setCompletedAt(LocalDateTime.now());
            transactionHistoryRepository.save(transaction);
            
            // 11. Record transaction fee
            recordTransactionFee(transaction.getTransactionId(), fee, senderWallet.getWallet().getCurrencyCode());
            
            // Create audit log
            createAuditLog(transaction, senderId.intValue(), "TRANSFER_COMPLETED", null);
            
            // 12. Send notifications
            String senderFirstName = sender.getFirstName() != null ? sender.getFirstName() : "User";
            String receiverFirstName = receiver.getFirstName() != null ? receiver.getFirstName() : "User";
            String currencyCode = senderWallet.getWallet().getCurrencyCode();
            String transactionIdStr = transaction.getTransactionId().toString();
            String currentDate = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            
            // FCM message for sender
            String fcmSenderMessage = "You Sent " + amount + " " + currencyCode + " to " + receiverFirstName;
            
            // SMS message for sender
            String smsSenderMessage = "Hello " + senderFirstName + ", you sent " + amount + " " + currencyCode + 
                " to " + receiverFirstName + " at " + currentDate + ". Transaction id is " + transactionIdStr;
            
            // Send FCM notification to sender (but not SMS - we'll send that directly)
            notificationService.sendFcmNotificationToSender(
                senderId.intValue(),
                "Payment Sent",
                fcmSenderMessage,
                transaction.getTransactionId()
            );
            
            // Send SMS directly to sender to avoid duplicate notifications
            try {
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
                // Don't fail the transaction if SMS fails
            }
            
            // FCM message for receiver
            String fcmReceiverMessage = "You Received " + amount + " " + currencyCode + " from " + senderFirstName;
            
            // SMS message for receiver
            String smsReceiverMessage = "Hello " + receiverFirstName + ", you received " + amount + " " + currencyCode + 
                " from " + senderFirstName + " at " + currentDate + ". Transaction id is " + transactionIdStr;
            
            log.info("Sending notification to receiver with FCM message: {}", fcmReceiverMessage);
            log.info("Sending notification to receiver with SMS message: {}", smsReceiverMessage);
            
            notificationService.sendTransactionNotificationToReceiver(
                receiver.getId().intValue(),
                "Payment Received",
                fcmReceiverMessage,
                transaction.getTransactionId(),
                smsReceiverMessage
            );
            
            // Create idempotency key if provided
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                IdempotencyKey newKey = new IdempotencyKey();
                newKey.setKey(idempotencyKey);
                newKey.setTransactionId(transaction.getTransactionId());
                newKey.setExpiresAt(LocalDateTime.now().plusDays(1));
                idempotencyKeyRepository.save(newKey);
            }
            
            // Process all pending notifications immediately to ensure they're sent right away
            notificationProcessor.processAllPendingNotificationsNow();
            
            // 13. Build and return response
            return WalletTransferResponse.builder()
                .status("success")
                .transactionId(transaction.getTransactionId())
                .senderWalletId(senderWallet.getId().longValue())
                .receiverWalletId(receiverWallet.getId().longValue())
                .amount(amount)
                .fee(fee)
                .totalAmount(totalAmount)
                .currency(senderWallet.getWallet().getCurrencyCode())
                .description(description)
                .timestamp(transaction.getCreatedAt())
                .senderWalletBalanceAfter(senderWallet.getBalance())
                .receiverName(receiver.getFirstName() + " " + receiver.getLastName())
                .build();
            
        } catch (Exception e) {
            log.error("Error processing wallet transfer by worker ID", e);
            throw e;
        }
    }
    
    /**
     * Calculate transaction fee based on amount and wallet type
     * 
     * @param amount The transaction amount
     * @param walletTypeId The wallet type ID
     * @return The calculated fee
     */
    private BigDecimal calculateTransactionFee(BigDecimal amount, Integer walletTypeId) {
        // Implementation depends on your fee structure
        // This is a simple example with a 2% fee
        return amount.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Record transaction fee
     * 
     * @param transactionId The transaction ID
     * @param feeAmount The fee amount
     * @param currency The currency code
     */
    private void recordTransactionFee(Long transactionId, BigDecimal feeAmount, String currency) {
        try {
            // Get fee type for wallet transfer
            FeeType feeType = feeTypeRepository.findByName("REMITTANCE_FEE")
                .orElseThrow(() -> new NotFoundException(ErrorCode.FEE_TYPE_NOT_FOUND));
            
            // Create transaction fee record
            TransactionFee fee = new TransactionFee();
            fee.setTransactionId(transactionId);
            fee.setFeeTypeId(feeType.getId());
            fee.setAmount(feeAmount);
            fee.setCurrency(currency);
            fee.setPercentage(new BigDecimal("2.00")); // 2% fee
            fee.setFixedAmount(BigDecimal.ZERO);
            fee.setCreatedAt(LocalDateTime.now());
            
            transactionFeeRepository.save(fee);
            
            log.info("Recorded transaction fee: transactionId={}, amount={}, currency={}", 
                    transactionId, feeAmount, currency);
            
        } catch (Exception e) {
            log.error("Error recording transaction fee: {}", e.getMessage(), e);
            // Don't throw exception, just log it
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
            auditLog.setAction(action);
            auditLog.setPerformedBy(performedBy);
            auditLog.setIpAddress(ipAddress);
            auditLog.setCreatedAt(LocalDateTime.now());
            
            // Set JSON fields to null to avoid type conversion issues
            auditLog.setOldState(null);
            auditLog.setNewState(null);
            
            transactionAuditLogRepository.save(auditLog);
            
            log.info("Created audit log for transaction ID: {}, action: {}", transaction.getTransactionId(), action);
        } catch (Exception e) {
            log.error("Error creating audit log: {}", e.getMessage(), e);
            // Don't throw exception, just log it
        }
    }
} 
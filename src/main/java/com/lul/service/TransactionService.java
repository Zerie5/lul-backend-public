package com.lul.service;

import com.lul.dto.WalletTransferRequest;
import com.lul.dto.WalletTransferResponse;
import com.lul.entity.*;
import com.lul.exception.InsufficientFundsException;
import com.lul.exception.InvalidPinException;
import com.lul.exception.NotFoundException;
import com.lul.exception.TransactionException;
import com.lul.repository.*;
import com.lul.constant.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final UserWalletRepository userWalletRepository;
    private final UserRepository userRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final FeeTypeRepository feeTypeRepository;
    private final FeeConfigurationRepository feeConfigurationRepository;
    private final TransactionFeeRepository transactionFeeRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final UserTransactionLimitRepository userTransactionLimitRepository;
    private final TransactionLimitHistoryRepository transactionLimitHistoryRepository;
    private final TransactionAuditLogRepository transactionAuditLogRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Process a wallet-to-wallet transfer
     * 
     * @param userId The ID of the user initiating the transfer
     * @param request The transfer request details
     * @return The transfer response with transaction details
     */
    @Transactional
    public WalletTransferResponse transferBetweenWallets(Long userId, WalletTransferRequest request) {
        log.info("Processing wallet-to-wallet transfer for user ID: {}", userId);
        
        // Check idempotency key if provided
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isEmpty()) {
            Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findByKey(request.getIdempotencyKey());
            if (existingKey.isPresent()) {
                log.info("Idempotent request detected with key: {}", request.getIdempotencyKey());
                // Return the existing transaction details
                TransactionHistory existingTransaction = transactionHistoryRepository
                    .findByTransactionId(existingKey.get().getTransactionId())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.TRANSACTION_NOT_FOUND));
                
                return buildTransferResponse(existingTransaction);
            }
        }
        
        // Get user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        
        // Verify PIN
        if (!passwordEncoder.matches(request.getPin(), user.getPinHash())) {
            log.warn("Invalid PIN provided for user ID: {}", userId);
            throw new InvalidPinException(ErrorCode.PIN_VERIFICATION_FAILED);
        }
        
        // Get sender wallet
        UserWallet senderWallet = userWalletRepository.findById(request.getSenderWalletId().intValue())
            .orElseThrow(() -> new NotFoundException(ErrorCode.WALLET_NOT_FOUND));
        
        // Verify sender wallet belongs to user
        if (!senderWallet.getUserId().equals(userId)) {
            log.warn("User ID: {} attempted to use wallet ID: {} which doesn't belong to them", userId, request.getSenderWalletId());
            throw new TransactionException(ErrorCode.UNAUTHORIZED_WALLET_ACCESS);
        }
        
        // Get receiver wallet
        UserWallet receiverWallet = userWalletRepository.findById(request.getReceiverWalletId().intValue())
            .orElseThrow(() -> new NotFoundException(ErrorCode.WALLET_NOT_FOUND));
        
        // Get receiver user
        User receiverUser = userRepository.findById(receiverWallet.getUserId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        
        // Verify wallets have same currency
        if (!senderWallet.getWallet().getCurrencyCode().equals(receiverWallet.getWallet().getCurrencyCode())) {
            log.warn("Currency mismatch between sender wallet: {} and receiver wallet: {}", 
                    senderWallet.getWallet().getCurrencyCode(), receiverWallet.getWallet().getCurrencyCode());
            throw new TransactionException(ErrorCode.CURRENCY_MISMATCH);
        }
        
        // Calculate fee
        BigDecimal fee = calculateTransferFee(request.getAmount());
        BigDecimal totalAmount = request.getAmount().add(fee);
        
        // Check if sender has sufficient balance
        if (senderWallet.getBalance().compareTo(totalAmount) < 0) {
            log.warn("Insufficient funds in wallet ID: {}. Required: {}, Available: {}", 
                    request.getSenderWalletId(), totalAmount, senderWallet.getBalance());
            throw new InsufficientFundsException(ErrorCode.INSUFFICIENT_FUNDS);
        }
        
        // Check transaction limits
        checkTransactionLimits(userId, request.getAmount(), senderWallet.getWallet().getCurrencyCode());
        
        // Get transaction type for wallet-to-wallet
        TransactionType transactionType = transactionTypeRepository.findByTypeName("WALLET_TO_WALLET")
            .orElseThrow(() -> new NotFoundException(ErrorCode.TRANSACTION_TYPE_NOT_FOUND));
        
        // Get pending transaction status
        TransactionStatus pendingStatus = transactionStatusRepository.findById(1)
            .orElseThrow(() -> new NotFoundException(ErrorCode.TRANSACTION_STATUS_NOT_FOUND));
        
        // Create transaction record
        TransactionHistory transaction = new TransactionHistory();
        transaction.setSenderId(userId.intValue());
        transaction.setReceiverId(receiverWallet.getUserId().intValue());
        transaction.setSenderWalletId(senderWallet.getId());
        transaction.setReceiverWalletId(receiverWallet.getId());
        transaction.setTransactionTypeId(transactionType.getId());
        transaction.setTransactionStatusId(pendingStatus.getId());
        transaction.setTransactedValue(request.getAmount());
        transaction.setCurrency(senderWallet.getWallet().getCurrencyCode());
        transaction.setDescription(request.getDescription());
        
        transaction = transactionHistoryRepository.save(transaction);
        
        // Create idempotency key if provided
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isEmpty()) {
            IdempotencyKey idempotencyKey = new IdempotencyKey();
            idempotencyKey.setKey(request.getIdempotencyKey());
            idempotencyKey.setTransactionId(transaction.getTransactionId());
            idempotencyKey.setExpiresAt(LocalDateTime.now().plusDays(1));
            idempotencyKeyRepository.save(idempotencyKey);
        }
        
        // Record fee
        recordTransactionFee(transaction, fee);
        
        // Update transaction limits
        updateTransactionLimits(userId, request.getAmount(), senderWallet.getWallet().getCurrencyCode(), transaction.getTransactionId());
        
        // Update wallet balances
        senderWallet.setBalance(senderWallet.getBalance().subtract(totalAmount));
        receiverWallet.setBalance(receiverWallet.getBalance().add(request.getAmount()));
        
        userWalletRepository.save(senderWallet);
        userWalletRepository.save(receiverWallet);
        
        // Update transaction status to completed
        TransactionStatus completedStatus = transactionStatusRepository.findById(2)
            .orElseThrow(() -> new NotFoundException(ErrorCode.TRANSACTION_STATUS_NOT_FOUND));
        transaction.setTransactionStatusId(completedStatus.getId());
        transaction = transactionHistoryRepository.save(transaction);
        
        // Create audit log
        createAuditLog(transaction, userId.intValue(), "TRANSFER_COMPLETED", null);
        
        // Queue notifications
        queueTransferNotifications(transaction, user, receiverUser);
        
        // Build and return response
        return WalletTransferResponse.builder()
                .status("success")
                .transactionId(transaction.getTransactionId())
                .senderWalletId(Long.valueOf(senderWallet.getId()))
                .receiverWalletId(Long.valueOf(receiverWallet.getId()))
                .amount(request.getAmount())
                .fee(fee)
                .totalAmount(totalAmount)
                .currency(senderWallet.getWallet().getCurrencyCode())
                .description(request.getDescription())
                .timestamp(transaction.getCreatedAt())
                .senderWalletBalanceAfter(senderWallet.getBalance())
                .receiverName(receiverUser.getFirstName() + " " + receiverUser.getLastName())
                .build();
    }
    
    /**
     * Calculate the fee for a wallet-to-wallet transfer
     * 
     * @param amount The transfer amount
     * @return The calculated fee
     */
    private BigDecimal calculateTransferFee(BigDecimal amount) {
        // Get fee configuration for wallet-to-wallet transfers
        FeeType transferFeeType = feeTypeRepository.findByName("TRANSFER_FEE")
            .orElseThrow(() -> new NotFoundException(ErrorCode.FEE_TYPE_NOT_FOUND));
        
        FeeConfiguration feeConfig = feeConfigurationRepository.findByFeeTypeIdAndIsActive(transferFeeType.getId(), true)
            .orElseThrow(() -> new NotFoundException(ErrorCode.FEE_CONFIGURATION_NOT_FOUND));
        
        // Calculate fee: percentage + fixed amount
        BigDecimal percentageFee = amount.multiply(feeConfig.getPercentage().divide(new BigDecimal("100")));
        BigDecimal totalFee = percentageFee.add(feeConfig.getFixedAmount());
        
        return totalFee;
    }
    
    /**
     * Record the transaction fee
     * 
     * @param transaction The transaction
     * @param feeAmount The fee amount
     */
    private void recordTransactionFee(TransactionHistory transaction, BigDecimal feeAmount) {
        FeeType transferFeeType = feeTypeRepository.findByName("TRANSFER_FEE")
            .orElseThrow(() -> new NotFoundException(ErrorCode.FEE_TYPE_NOT_FOUND));
        
        FeeConfiguration feeConfig = feeConfigurationRepository.findByFeeTypeIdAndIsActive(transferFeeType.getId(), true)
            .orElseThrow(() -> new NotFoundException(ErrorCode.FEE_CONFIGURATION_NOT_FOUND));
        
        TransactionFee fee = new TransactionFee();
        fee.setTransactionId(transaction.getTransactionId());
        fee.setFeeTypeId(transferFeeType.getId());
        fee.setAmount(feeAmount);
        fee.setCurrency(transaction.getCurrency());
        fee.setPercentage(feeConfig.getPercentage());
        fee.setFixedAmount(feeConfig.getFixedAmount());
        
        transactionFeeRepository.save(fee);
    }
    
    /**
     * Check if the transaction exceeds user's limits
     * 
     * @param userId The user ID
     * @param amount The transaction amount
     * @param currency The currency
     */
    private void checkTransactionLimits(Long userId, BigDecimal amount, String currency) {
        UserTransactionLimit limits = userTransactionLimitRepository.findByUserId(userId.intValue())
            .orElseThrow(() -> new NotFoundException(ErrorCode.TRANSACTION_LIMITS_NOT_FOUND));
        
        // Check if transaction amount exceeds max transaction amount
        if (amount.compareTo(limits.getKycLevel().getMaxTransactionAmount()) > 0) {
            log.warn("Transaction amount {} exceeds max transaction amount {} for user ID: {}", 
                    amount, limits.getKycLevel().getMaxTransactionAmount(), userId);
            throw new TransactionException(ErrorCode.TRANSACTION_LIMIT_EXCEEDED);
        }
        
        // Check if transaction amount is below min transaction amount
        if (amount.compareTo(limits.getKycLevel().getMinTransactionAmount()) < 0) {
            log.warn("Transaction amount {} is below min transaction amount {} for user ID: {}", 
                    amount, limits.getKycLevel().getMinTransactionAmount(), userId);
            throw new TransactionException(ErrorCode.TRANSACTION_AMOUNT_TOO_SMALL);
        }
        
        // Check daily limit
        if (limits.getDailyUsed().add(amount).compareTo(limits.getDailyLimit()) > 0) {
            log.warn("Transaction would exceed daily limit for user ID: {}. Current usage: {}, Limit: {}", 
                    userId, limits.getDailyUsed(), limits.getDailyLimit());
            throw new TransactionException(ErrorCode.DAILY_LIMIT_EXCEEDED);
        }
        
        // Check monthly limit
        if (limits.getMonthlyUsed().add(amount).compareTo(limits.getMonthlyLimit()) > 0) {
            log.warn("Transaction would exceed monthly limit for user ID: {}. Current usage: {}, Limit: {}", 
                    userId, limits.getMonthlyUsed(), limits.getMonthlyLimit());
            throw new TransactionException(ErrorCode.MONTHLY_LIMIT_EXCEEDED);
        }
        
        // Check annual limit
        if (limits.getAnnualUsed().add(amount).compareTo(limits.getAnnualLimit()) > 0) {
            log.warn("Transaction would exceed annual limit for user ID: {}. Current usage: {}, Limit: {}", 
                    userId, limits.getAnnualUsed(), limits.getAnnualLimit());
            throw new TransactionException(ErrorCode.ANNUAL_LIMIT_EXCEEDED);
        }
    }
    
    /**
     * Update the user's transaction limits
     * 
     * @param userId The user ID
     * @param amount The transaction amount
     * @param currency The currency
     * @param transactionId The transaction ID
     */
    private void updateTransactionLimits(Long userId, BigDecimal amount, String currency, Long transactionId) {
        UserTransactionLimit limits = userTransactionLimitRepository.findByUserId(userId.intValue())
            .orElseThrow(() -> new NotFoundException(ErrorCode.TRANSACTION_LIMITS_NOT_FOUND));
        
        // Update daily used
        BigDecimal previousDailyUsed = limits.getDailyUsed();
        limits.setDailyUsed(limits.getDailyUsed().add(amount));
        
        // Update monthly used
        BigDecimal previousMonthlyUsed = limits.getMonthlyUsed();
        limits.setMonthlyUsed(limits.getMonthlyUsed().add(amount));
        
        // Update annual used
        BigDecimal previousAnnualUsed = limits.getAnnualUsed();
        limits.setAnnualUsed(limits.getAnnualUsed().add(amount));
        
        // Update last reset timestamps if needed
        LocalDateTime now = LocalDateTime.now();
        if (limits.getLastResetDaily() == null) {
            limits.setLastResetDaily(now);
        }
        if (limits.getLastResetMonthly() == null) {
            limits.setLastResetMonthly(now);
        }
        if (limits.getLastResetAnnual() == null) {
            limits.setLastResetAnnual(now);
        }
        
        userTransactionLimitRepository.save(limits);
        
        // Record limit history for daily limit
        TransactionLimitHistory dailyHistory = new TransactionLimitHistory();
        dailyHistory.setUserId(userId.intValue());
        dailyHistory.setTransactionId(transactionId);
        dailyHistory.setAmount(amount);
        dailyHistory.setCurrency(currency);
        dailyHistory.setLimitType("DAILY");
        dailyHistory.setPreviousUsed(previousDailyUsed);
        dailyHistory.setNewUsed(limits.getDailyUsed());
        dailyHistory.setLimitValue(limits.getDailyLimit());
        transactionLimitHistoryRepository.save(dailyHistory);
        
        // Record limit history for monthly limit
        TransactionLimitHistory monthlyHistory = new TransactionLimitHistory();
        monthlyHistory.setUserId(userId.intValue());
        monthlyHistory.setTransactionId(transactionId);
        monthlyHistory.setAmount(amount);
        monthlyHistory.setCurrency(currency);
        monthlyHistory.setLimitType("MONTHLY");
        monthlyHistory.setPreviousUsed(previousMonthlyUsed);
        monthlyHistory.setNewUsed(limits.getMonthlyUsed());
        monthlyHistory.setLimitValue(limits.getMonthlyLimit());
        transactionLimitHistoryRepository.save(monthlyHistory);
        
        // Record limit history for annual limit
        TransactionLimitHistory annualHistory = new TransactionLimitHistory();
        annualHistory.setUserId(userId.intValue());
        annualHistory.setTransactionId(transactionId);
        annualHistory.setAmount(amount);
        annualHistory.setCurrency(currency);
        annualHistory.setLimitType("ANNUAL");
        annualHistory.setPreviousUsed(previousAnnualUsed);
        annualHistory.setNewUsed(limits.getAnnualUsed());
        annualHistory.setLimitValue(limits.getAnnualLimit());
        transactionLimitHistoryRepository.save(annualHistory);
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
        TransactionAuditLog auditLog = new TransactionAuditLog();
        auditLog.setTransactionId(transaction.getTransactionId());
        auditLog.setAction(action);
        auditLog.setPerformedBy(performedBy);
        auditLog.setIpAddress(ipAddress);
        
        transactionAuditLogRepository.save(auditLog);
    }
    
    /**
     * Queue notifications for the sender and receiver
     * 
     * @param transaction The transaction
     * @param sender The sender user
     * @param receiver The receiver user
     */
    private void queueTransferNotifications(TransactionHistory transaction, User sender, User receiver) {
        // Queue notification for sender
        notificationService.queueTransactionNotification(
            sender.getId().intValue(),
            "TRANSACTION_SENT",
            "Transfer Sent",
            String.format("You have sent %s %s to %s %s", 
                transaction.getTransactedValue(), 
                transaction.getCurrency(),
                receiver.getFirstName(),
                receiver.getLastName()),
            transaction.getTransactionId()
        );
        
        // Queue notification for receiver
        notificationService.queueTransactionNotification(
            receiver.getId().intValue(),
            "TRANSACTION_RECEIVED",
            "Transfer Received",
            String.format("You have received %s %s from %s %s", 
                transaction.getTransactedValue(), 
                transaction.getCurrency(),
                sender.getFirstName(),
                sender.getLastName()),
            transaction.getTransactionId()
        );
    }
    
    /**
     * Build a transfer response from a transaction
     * 
     * @param transaction The transaction
     * @return The transfer response
     */
    private WalletTransferResponse buildTransferResponse(TransactionHistory transaction) {
        // Get fee
        Optional<TransactionFee> fee = transactionFeeRepository.findByTransactionId(transaction.getTransactionId());
        BigDecimal feeAmount = fee.map(TransactionFee::getAmount).orElse(BigDecimal.ZERO);
        
        // Get sender wallet
        UserWallet senderWallet = userWalletRepository.findById(transaction.getSenderWalletId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.WALLET_NOT_FOUND));
        
        // Get receiver user
        User receiverUser = userRepository.findById(Long.valueOf(transaction.getReceiverId()))
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        
        return WalletTransferResponse.builder()
                .status("success")
                .transactionId(transaction.getTransactionId())
                .senderWalletId(Long.valueOf(transaction.getSenderWalletId()))
                .receiverWalletId(Long.valueOf(transaction.getReceiverWalletId()))
                .amount(transaction.getTransactedValue())
                .fee(feeAmount)
                .totalAmount(transaction.getTransactedValue().add(feeAmount))
                .currency(transaction.getCurrency())
                .description(transaction.getDescription())
                .timestamp(transaction.getCreatedAt())
                .senderWalletBalanceAfter(senderWallet.getBalance())
                .receiverName(receiverUser.getFirstName() + " " + receiverUser.getLastName())
                .build();
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
    public WalletTransferResponse transferByWorkerId(
            Long senderId, 
            Integer senderWalletTypeId,
            String receiverWorkerId,
            BigDecimal amount,
            String pin,
            String description,
            String idempotencyKey) {
        // Implementation of transferByWorkerId method
        // This method needs to be implemented based on the specific requirements
        throw new UnsupportedOperationException("Method not implemented");
    }
} 
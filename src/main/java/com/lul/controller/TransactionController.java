package com.lul.controller;

import com.lul.constant.ErrorCode;
import com.lul.dto.WalletTransferByWorkerIdRequest;
import com.lul.dto.WalletTransferResponse;
import com.lul.entity.User;
import com.lul.exception.InsufficientFundsException;
import com.lul.exception.InvalidPinException;
import com.lul.exception.NotFoundException;
import com.lul.exception.TransactionLimitExceededException;
import com.lul.service.WorkerIdTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling transaction-related operations
 * 
 * Note on SMS notifications: SMS messages to receivers are kept under 50 characters
 * to ensure compatibility with all mobile carriers and avoid message splitting.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final WorkerIdTransferService transferService;

    /**
     * Endpoint for wallet-to-wallet transfer using worker ID
     * This simplifies the frontend implementation by allowing transfers using worker ID as an "account number"
     */
    @PostMapping("/wallet-to-wallet")
    public ResponseEntity<?> transferBetweenWallets(@Valid @RequestBody WalletTransferByWorkerIdRequest request) {
        try {
            // Get authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            
            // Execute the transfer
            WalletTransferResponse response = transferService.transferByWorkerId(
                user.getId(), 
                request.getSenderWalletTypeId(),
                request.getReceiverWorkerId(),
                request.getAmount(),
                request.getPin(),
                request.getDescription(),
                request.getIdempotencyKey()
            );
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", response
            ));
        } catch (InvalidPinException e) {
            log.warn("Invalid PIN attempt for user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "status", "error",
                    "code", ErrorCode.INVALID_PIN.getCode(),
                    "message", "Invalid PIN"
                ));
        } catch (InsufficientFundsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "status", "error",
                    "code", ErrorCode.INSUFFICIENT_FUNDS.getCode(),
                    "message", "Insufficient funds"
                ));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "status", "error",
                    "code", e.getErrorCode().getCode(),
                    "message", e.getMessage()
                ));
        } catch (TransactionLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "status", "error",
                    "code", e.getErrorCode().getCode(),
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            log.error("Error processing wallet transfer: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "code", ErrorCode.TRANSACTION_FAILED.getCode(),
                    "message", "An unexpected error occurred"
                ));
        }
    }
    
    /**
     * Legacy endpoint for backward compatibility
     * Redirects to the main wallet-to-wallet endpoint
     */
    @PostMapping("/wallet-to-wallet-by-worker-id")
    public ResponseEntity<?> transferByWorkerId(@Valid @RequestBody WalletTransferByWorkerIdRequest request) {
        return transferBetweenWallets(request);
    }
} 
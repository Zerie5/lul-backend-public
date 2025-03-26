package com.lul.controller;

import com.lul.dto.ApiResponse;
import com.lul.dto.NonWalletTransferRequest;
import com.lul.dto.NonWalletTransferResponse;
import com.lul.entity.User;
import com.lul.service.NonWalletTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling non-wallet transfer operations.
 * This controller provides endpoints for transferring funds to recipients who don't have a wallet.
 */
@RestController
@RequestMapping("/api/v1/transfers/non-wallet")
@RequiredArgsConstructor
@Slf4j
public class NonWalletTransferController {

    private final NonWalletTransferService nonWalletTransferService;

    /**
     * Endpoint to initiate a non-wallet transfer.
     * This endpoint allows a user to transfer funds to a recipient who doesn't have a wallet.
     *
     * @param userDetails The authenticated user details
     * @param request     The non-wallet transfer request containing all necessary details
     * @return ResponseEntity containing the transfer response
     */
    @PostMapping
    public ResponseEntity<ApiResponse<NonWalletTransferResponse>> initiateNonWalletTransfer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NonWalletTransferRequest request) {
        
        log.info("Received non-wallet transfer request: {}", request);
        
        // Extract user ID from authenticated user
        Long userId;
        if (userDetails instanceof User) {
            userId = ((User) userDetails).getId();
        } else {
            throw new IllegalStateException("User details not of expected type");
        }
        
        // Process the transfer
        NonWalletTransferResponse response = nonWalletTransferService.transferToNonWallet(userId, request);
        
        // Return success response
        return ResponseEntity.ok(new ApiResponse<>(true, "Non-wallet transfer successful", response));
    }

    /**
     * Endpoint to get the status of a non-wallet transfer.
     * This endpoint allows a user to check the status of a previously initiated non-wallet transfer.
     *
     * @param userDetails   The authenticated user details
     * @param transactionId The ID of the transaction to check
     * @return ResponseEntity containing the transfer status
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<NonWalletTransferResponse>> getNonWalletTransferStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long transactionId) {
        
        log.info("Received request to check non-wallet transfer status for transaction ID: {}", transactionId);
        
        // Extract user ID from authenticated user
        Long userId;
        if (userDetails instanceof User) {
            userId = ((User) userDetails).getId();
        } else {
            throw new IllegalStateException("User details not of expected type");
        }
        
        // Get the transfer status
        NonWalletTransferResponse response = nonWalletTransferService.getNonWalletTransferStatus(userId, transactionId);
        
        // Return success response
        return ResponseEntity.ok(new ApiResponse<>(true, "Non-wallet transfer status retrieved successfully", response));
    }
} 
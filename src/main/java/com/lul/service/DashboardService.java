package com.lul.service;

import com.lul.dto.DashboardSummaryDto;
import com.lul.repository.TransactionHistoryRepository;
import com.lul.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service for dashboard data and analytics
 */
@Service
@Slf4j
public class DashboardService {
    
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public DashboardService(
            TransactionHistoryRepository transactionHistoryRepository,
            UserRepository userRepository) {
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Get dashboard summary statistics
     * 
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @return DashboardSummaryDto containing all summary metrics
     */
    @Cacheable(value = "dashboardSummary", key = "#startDate + '-' + #endDate", condition = "#startDate != null && #endDate != null")
    public DashboardSummaryDto getDashboardSummary(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Calculating dashboard summary data for period: {} to {}", startDate, endDate);
        
        // Get total transactions
        long totalTransactions;
        BigDecimal totalTransactionValue;
        BigDecimal totalRevenue;
        
        if (startDate != null && endDate != null) {
            // Get filtered data for specific date range
            totalTransactions = transactionHistoryRepository.countByCreatedAtBetween(startDate, endDate);
            totalTransactionValue = transactionHistoryRepository.sumTransactedValueBetween(startDate, endDate);
            totalRevenue = transactionHistoryRepository.sumFeesBetween(startDate, endDate);
        } else {
            // Get all-time data
            totalTransactions = transactionHistoryRepository.count();
            totalTransactionValue = transactionHistoryRepository.sumTransactedValue();
            totalRevenue = transactionHistoryRepository.sumFees();
        }
        
        // Get active users (users who had activity in last 30 days)
        LocalDateTime activeUserCutoff = LocalDateTime.now().minusDays(30);
        long activeUsers = userRepository.countByUpdatedAtAfter(activeUserCutoff);
        
        log.info("Dashboard summary calculated: {} transactions, {} value, {} revenue, {} active users", 
                totalTransactions, totalTransactionValue, totalRevenue, activeUsers);
        
        // Create and return DTO
        return new DashboardSummaryDto(
            totalTransactions,
            totalTransactionValue != null ? totalTransactionValue : BigDecimal.ZERO,
            activeUsers,
            totalRevenue != null ? totalRevenue : BigDecimal.ZERO
        );
    }
} 
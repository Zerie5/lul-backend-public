package com.lul.service;

import com.lul.dto.DashboardSummaryDto;
import com.lul.repository.TransactionHistoryRepository;
import com.lul.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    @Mock
    private TransactionHistoryRepository transactionHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getDashboardSummary_AllTime_ShouldReturnCorrectValues() {
        // Arrange
        when(transactionHistoryRepository.count()).thenReturn(1248L);
        when(transactionHistoryRepository.sumTransactedValue()).thenReturn(new BigDecimal("87654.32"));
        when(transactionHistoryRepository.sumFees()).thenReturn(new BigDecimal("4328.75"));
        when(userRepository.countByUpdatedAtAfter(any())).thenReturn(356L);

        // Act
        DashboardSummaryDto result = dashboardService.getDashboardSummary(null, null);

        // Assert
        assertEquals(1248L, result.getTotalTransactions());
        assertEquals(new BigDecimal("87654.32"), result.getTotalTransactionValue());
        assertEquals(356L, result.getActiveUsers());
        assertEquals(new BigDecimal("4328.75"), result.getTotalRevenue());
    }

    @Test
    void getDashboardSummary_DateRange_ShouldReturnFilteredValues() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);

        when(transactionHistoryRepository.countByCreatedAtBetween(startDate, endDate)).thenReturn(500L);
        when(transactionHistoryRepository.sumTransactedValueBetween(startDate, endDate)).thenReturn(new BigDecimal("30000.00"));
        when(transactionHistoryRepository.sumFeesBetween(startDate, endDate)).thenReturn(new BigDecimal("1500.00"));
        when(userRepository.countByUpdatedAtAfter(any())).thenReturn(300L);

        // Act
        DashboardSummaryDto result = dashboardService.getDashboardSummary(startDate, endDate);

        // Assert
        assertEquals(500L, result.getTotalTransactions());
        assertEquals(new BigDecimal("30000.00"), result.getTotalTransactionValue());
        assertEquals(300L, result.getActiveUsers());
        assertEquals(new BigDecimal("1500.00"), result.getTotalRevenue());
    }

    @Test
    void getDashboardSummary_NullValues_ShouldHandleGracefully() {
        // Arrange
        when(transactionHistoryRepository.count()).thenReturn(0L);
        when(transactionHistoryRepository.sumTransactedValue()).thenReturn(null);
        when(transactionHistoryRepository.sumFees()).thenReturn(null);
        when(userRepository.countByUpdatedAtAfter(any())).thenReturn(0L);

        // Act
        DashboardSummaryDto result = dashboardService.getDashboardSummary(null, null);

        // Assert
        assertEquals(0L, result.getTotalTransactions());
        assertEquals(BigDecimal.ZERO, result.getTotalTransactionValue());
        assertEquals(0L, result.getActiveUsers());
        assertEquals(BigDecimal.ZERO, result.getTotalRevenue());
    }
} 
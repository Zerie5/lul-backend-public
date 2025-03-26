package com.lul.controller.admin;

import com.lul.dto.DashboardSummaryDto;
import com.lul.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Controller for admin dashboard data
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@Slf4j
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    
    /**
     * Get dashboard summary data (total transactions, total value, active users, total revenue)
     * 
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @return Dashboard summary data
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Getting dashboard summary for period: {} to {}", startDate, endDate);
        
        // Ensure endDate is inclusive of the whole day if provided
        if (endDate != null) {
            endDate = endDate.plusDays(1).minusNanos(1);
        }
        
        DashboardSummaryDto summary = dashboardService.getDashboardSummary(startDate, endDate);
        return ResponseEntity.ok(summary);
    }
} 
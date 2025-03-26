package com.lul.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service to manage cache eviction
 */
@Service
@Slf4j
public class CacheEvictionService {

    private final CacheManager cacheManager;

    @Autowired
    public CacheEvictionService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Clear dashboard cache every 15 minutes to ensure data freshness
     */
    @Scheduled(fixedRate = 900000) // 15 minutes in milliseconds
    public void evictDashboardCache() {
        log.info("Evicting dashboard summary cache");
        cacheManager.getCache("dashboardSummary").clear();
    }
} 
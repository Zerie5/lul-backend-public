package com.lul.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for caching
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure cache manager with appropriate cache names
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "dashboardSummary"
            // Add other cache names as needed
        );
    }
} 
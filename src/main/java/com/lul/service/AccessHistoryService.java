package com.lul.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import com.lul.entity.AccessHistory;
import com.lul.repository.AccessHistoryRepository;
import com.lul.dto.LocationInfo;
import com.lul.enums.RiskLevel;


@Service
@Slf4j
@Transactional
public class AccessHistoryService {
    
    private final AccessHistoryRepository accessHistoryRepository;
    
    @Autowired
    private IpGeolocationService ipGeolocationService;
    
    @Autowired
    public AccessHistoryService(AccessHistoryRepository accessHistoryRepository) {
        this.accessHistoryRepository = accessHistoryRepository;
    }
    
    @Transactional
    public void recordAccess(AccessHistory accessHistory) {
        // Get location info from IP
        LocationInfo locationInfo = ipGeolocationService.getLocation(accessHistory.getIpAddress());
        
        // Set location info
        accessHistory.setCity(locationInfo.getCity());
        accessHistory.setCountry(locationInfo.getCountry());
        
        // Set timestamp
        accessHistory.setLastAccessed(LocalDateTime.now());
        
        // Set all existing sessions to false
        accessHistoryRepository.updateIsCurrentSessionByUserId(
            accessHistory.getUserId(), 
            false
        );
        
        // Set this as current session
        accessHistory.setCurrentSession(true);
        
        // Save new session
        accessHistoryRepository.save(accessHistory);
    }
    
    public List<Map<String, Object>> getUserAccessHistory(Long userId) {
        List<AccessHistory> history = accessHistoryRepository.findByUserIdOrderByLastAccessedDesc(userId);
        List<Map<String, Object>> formattedHistory = new ArrayList<>();
        
        for (AccessHistory entry : history) {
            Map<String, Object> historyEntry = new HashMap<>();
            historyEntry.put("os", entry.getOs());
            historyEntry.put("deviceName", entry.getDeviceName());
            historyEntry.put("city", entry.getCity() != null ? entry.getCity() : "Unknown");
            historyEntry.put("country", entry.getCountry() != null ? entry.getCountry() : "Unknown");
            historyEntry.put("ipAddress", entry.getIpAddress());
            historyEntry.put("deviceId", entry.getDeviceId());
            historyEntry.put("isCurrentSession", entry.isCurrentSession());
            
            // Format timestamp in ISO-8601 with UTC timezone
            // Handle null timestamps gracefully
            LocalDateTime timestamp = entry.getLastAccessed();
            if (timestamp == null) {
                timestamp = entry.getAccessTime(); // Try to use accessTime as fallback
            }
            
            if (timestamp != null) {
                String formattedTimestamp = timestamp
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                historyEntry.put("accessTime", formattedTimestamp);
            } else {
                // If both timestamps are null, use current time as a fallback
                historyEntry.put("accessTime", LocalDateTime.now()
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
            
            formattedHistory.add(historyEntry);
        }
        
        return formattedHistory;
    }

    public AccessHistory save(AccessHistory accessHistory) {
        return accessHistoryRepository.save(accessHistory);
    }
    
    /**
     * Check if a device is known for a user based on device fingerprint
     * @param userId The user ID
     * @param deviceFingerprint The device fingerprint
     * @return true if the device is known, false otherwise
     */
    public boolean isKnownDevice(Long userId, String deviceFingerprint) {
        return accessHistoryRepository.existsByUserIdAndDeviceFingerprint(userId, deviceFingerprint);
    }
    
    /**
     * Assess the risk level of a login attempt based on device recognition
     * @param userId The user ID
     * @param deviceFingerprint The device fingerprint
     * @return The risk level (LOW for known devices, MEDIUM for unknown devices)
     */
    public RiskLevel assessRiskLevel(Long userId, String deviceFingerprint) {
        boolean knownDevice = isKnownDevice(userId, deviceFingerprint);
        return knownDevice ? RiskLevel.LOW : RiskLevel.MEDIUM;
    }
} 
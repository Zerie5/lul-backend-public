package com.lul.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.net.InetAddress;
import lombok.extern.slf4j.Slf4j;
import com.lul.dto.LocationInfo;

@Service
@Slf4j
public class IpGeolocationService {
    private DatabaseReader reader;

    @PostConstruct
    public void init() {
        try {
            // Load GeoLite2 database from resources
            InputStream database = new ClassPathResource("GeoLite2-City.mmdb").getInputStream();
            reader = new DatabaseReader.Builder(database).build();
            log.info("Successfully initialized GeoIP database");
        } catch (Exception e) {
            log.error("Failed to initialize GeoIP database", e);
        }
    }

    public LocationInfo getLocation(String ipAddress) {
        try {
            // Skip lookup for private/local IPs
            if (isPrivateIP(ipAddress)) {
                return LocationInfo.builder()
                    .city("Local Network")
                    .country("Local Network")
                    .build();
            }

            CityResponse response = reader.city(InetAddress.getByName(ipAddress));
            return LocationInfo.builder()
                .city(response.getCity().getName() != null ? response.getCity().getName() : "Unknown")
                .country(response.getCountry().getName() != null ? response.getCountry().getName() : "Unknown")
                .build();
        } catch (Exception e) {
            log.error("Failed to get location for IP: {}", ipAddress, e);
            return LocationInfo.builder()
                .city("Unknown")
                .country("Unknown")
                .build();
        }
    }

    private boolean isPrivateIP(String ipAddress) {
        try {
            InetAddress ip = InetAddress.getByName(ipAddress);
            return ip.isSiteLocalAddress() || ip.isLoopbackAddress() || ip.isLinkLocalAddress();
        } catch (Exception e) {
            return true;
        }
    }
} 
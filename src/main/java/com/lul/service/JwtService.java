package com.lul.service;

import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import io.jsonwebtoken.io.Decoders;

import java.security.Key;

@Service
public class JwtService {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    private Key getSignKey() {
        try {
            // Use Base64 decoded key consistently
            byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
            Key key = Keys.hmacShaKeyFor(keyBytes);
            logger.debug("JWT signing key generated successfully");
            return key;
        } catch (Exception e) {
            logger.error("Error generating JWT signing key: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String validateTokenAndGetEmail(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

    public String generateToken(Map<String, Object> claims) {
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365)) // 365 days
            .signWith(getSignKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String extractUserId(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSignKey())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .setAllowedClockSkewSeconds(60) // Add 1 minute clock skew allowance
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
} 
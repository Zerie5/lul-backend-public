package com.lul.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.lul.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Auth endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                
                // User registration and verification
                .requestMatchers("/api/user/register").permitAll()
                .requestMatchers("/api/v1/user/register").permitAll()
                
                // OTP endpoints
                .requestMatchers("/api/v1/otp/**").permitAll()
                .requestMatchers("/api/otp/**").permitAll()
                
                // PIN verification endpoints
                .requestMatchers("/api/v1/user/verify-pin").permitAll()
                .requestMatchers("/api/v1/user/update-pin").authenticated()
                
                // Phone update endpoint
                .requestMatchers("/api/v1/user/update-phone").authenticated()
                
                // Error endpoints
                .requestMatchers("/error").permitAll()
                
                // Notification endpoint
                .requestMatchers("/api/notifications/fcm-token").authenticated()
                
                // User lookup endpoint (requires authentication)
                .requestMatchers("/api/user/lookup/**").authenticated()
                
                // Connectivity check endpoints (no authentication required)
                .requestMatchers("/api/connectivity/**").permitAll()
                
                // Auth check endpoints
                .requestMatchers("/api/auth-check/verify").authenticated()
                
                // User info endpoints
                .requestMatchers("/api/user-info/current").authenticated()
                .requestMatchers("/api/user-info/worker-id").authenticated()
                
                // Admin dashboard endpoints
                .requestMatchers("/api/admin/dashboard/**").permitAll() // Temporarily permit all for testing
                
                // Secure all other endpoints
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:3000");
        configuration.addAllowedOrigin("http://192.168.100.79:3000");
        configuration.addAllowedOrigin("https://admin.yourdomain.com");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        configuration.addExposedHeader("Authorization");
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

package com.lul.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Web configuration for CORS and other web-related settings
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure CORS settings for the application
     * This allows the React admin panel to access the APIs
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // Match all endpoints
                .allowedOrigins(
                    "http://localhost:3000", // Local React dev server
                    "http://192.168.100.79:3000", // Local React with IP
                    "https://admin.yourdomain.com" // Production admin panel URL
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")  // Allow Authorization header to be exposed to client
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

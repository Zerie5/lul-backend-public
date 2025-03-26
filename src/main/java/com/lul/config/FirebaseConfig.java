package com.lul.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.context.annotation.Conditional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseConfig {

    @Value("${firebase.credentials.path:firebase-service-account.json}")
    private String firebaseCredentialsPath;

    private boolean firebaseInitialized = false;

    @PostConstruct
    public void initialize() {
        try {
            Resource resource = new ClassPathResource(firebaseCredentialsPath);
            
            if (!resource.exists()) {
                log.error("Firebase credentials file not found at: {}. Firebase services will be disabled.", firebaseCredentialsPath);
                log.error("Please create a valid firebase-service-account.json file in the resources directory.");
                log.error("You can get this file from the Firebase Console > Project Settings > Service Accounts > Generate new private key");
                return;
            }
            
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                .build();
            
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                firebaseInitialized = true;
                log.info("Firebase has been initialized successfully");
            } else {
                firebaseInitialized = true;
                log.info("Firebase was already initialized");
            }
        } catch (IOException e) {
            log.error("Error initializing Firebase: {}", e.getMessage(), e);
            log.error("Please check your firebase-service-account.json file for proper formatting");
        } catch (Exception e) {
            log.error("Unexpected error initializing Firebase: {}", e.getMessage(), e);
        }
    }
    
    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (!firebaseInitialized) {
            log.warn("Firebase was not initialized. FirebaseMessaging bean will not be available.");
            return null;
        }
        try {
            return FirebaseMessaging.getInstance();
        } catch (Exception e) {
            log.error("Error creating FirebaseMessaging bean: {}", e.getMessage(), e);
            return null;
        }
    }
}

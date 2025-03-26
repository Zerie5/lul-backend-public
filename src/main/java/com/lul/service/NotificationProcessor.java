package com.lul.service;

import com.lul.entity.NotificationQueue;
import com.lul.entity.User;
import com.lul.entity.FcmToken;
import com.lul.repository.NotificationQueueRepository;
import com.lul.repository.UserRepository;
import com.lul.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;

/**
 * Service for processing queued notifications and sending them through appropriate channels
 */
@Service
@Slf4j
public class NotificationProcessor {

    private final NotificationQueueRepository notificationQueueRepository;
    private final UserRepository userRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final SmsService smsService;
    private FirebaseMessaging firebaseMessaging;

    @Autowired
    public NotificationProcessor(
            NotificationQueueRepository notificationQueueRepository,
            UserRepository userRepository,
            FcmTokenRepository fcmTokenRepository,
            SmsService smsService) {
        this.notificationQueueRepository = notificationQueueRepository;
        this.userRepository = userRepository;
        this.fcmTokenRepository = fcmTokenRepository;
        this.smsService = smsService;
        
        // Try to get FirebaseMessaging instance if available
        try {
            log.info("Initializing Firebase Messaging");
            this.firebaseMessaging = FirebaseMessaging.getInstance();
            log.info("Firebase Messaging initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Firebase Messaging: {}", e.getMessage(), e);
            log.warn("Push notifications via FCM will not be available");
            this.firebaseMessaging = null;
        }
    }

    /**
     * Process pending notifications every 5 seconds
     */
    @Scheduled(fixedRate = 5000) // Run every 5 seconds
    @Transactional
    public void processNotifications() {
        log.info("Starting notification processing job");
        
        try {
            // Find pending notifications that are ready to be processed
            List<NotificationQueue> pendingNotifications = notificationQueueRepository.findByStatusAndNextRetryAtBefore(
                "PENDING", LocalDateTime.now());
            
            log.info("Found {} pending notifications to process", pendingNotifications.size());
            
            if (pendingNotifications.isEmpty()) {
                // Check if there are any notifications in the queue at all
                long totalCount = notificationQueueRepository.count();
                log.info("Total notifications in queue: {}", totalCount);
                
                if (totalCount > 0) {
                    // Check notifications by status
                    List<NotificationQueue> allNotifications = notificationQueueRepository.findAll();
                    Map<String, Long> countByStatus = allNotifications.stream()
                        .collect(Collectors.groupingBy(NotificationQueue::getStatus, Collectors.counting()));
                    log.info("Notifications by status: {}", countByStatus);
                    
                    // Check if there are notifications that are not ready for retry yet
                    long notReadyCount = allNotifications.stream()
                        .filter(n -> "PENDING".equals(n.getStatus()) && n.getNextRetryAt().isAfter(LocalDateTime.now()))
                        .count();
                    log.info("Notifications pending but not ready for retry yet: {}", notReadyCount);
                }
            }
            
            for (NotificationQueue notification : pendingNotifications) {
                try {
                    log.info("Processing notification ID: {}, type: {}, channel: {}, user: {}", 
                        notification.getId(), 
                        notification.getNotificationType() != null ? notification.getNotificationType().getName() : "unknown",
                        notification.getChannel() != null ? notification.getChannel().getName() : "unknown",
                        notification.getUserId());
                    
                    // Process notification based on channel
                    boolean success = false;
                    
                    // If channel object is null but channel ID is valid, try to determine channel type from ID
                    if (notification.getChannel() == null) {
                        Integer channelId = notification.getChannelId();
                        log.info("Channel object is null but channel ID is: {}", channelId);
                        
                        if (channelId != null) {
                            // Map channel ID to type
                            String channelType = null;
                            switch (channelId) {
                                case 2:
                                    channelType = "FCM";
                                    break;
                                case 3:
                                    channelType = "SMS";
                                    break;
                                case 4:
                                    channelType = "PUSH";
                                    break;
                                case 5:
                                    channelType = "EMAIL";
                                    break;
                                case 6:
                                    channelType = "IN-APP";
                                    break;
                                default:
                                    log.error("Unknown channel ID: {}", channelId);
                            }
                            
                            if (channelType != null) {
                                log.info("Determined channel type '{}' from ID: {}", channelType, channelId);
                                
                                switch (channelType) {
                                    case "SMS":
                                        success = sendSmsNotification(notification);
                                        break;
                                    case "EMAIL":
                                        success = sendEmailNotification(notification);
                                        break;
                                    case "FCM":
                                    case "PUSH":
                                        success = sendPushNotification(notification);
                                        break;
                                    default:
                                        log.warn("Unsupported channel type: {}", channelType);
                                        break;
                                }
                            } else {
                                log.error("Could not determine channel type for ID: {}", channelId);
                                notification.setStatus("ERROR");
                                notification.setErrorMessage("Unknown channel ID: " + channelId);
                                notificationQueueRepository.save(notification);
                                continue;
                            }
                        } else {
                            log.error("Channel is null for notification ID: {}", notification.getId());
                            notification.setStatus("ERROR");
                            notification.setErrorMessage("Channel is null");
                            notificationQueueRepository.save(notification);
                            continue;
                        }
                    } else {
                        // Normal processing with channel object
                        switch (notification.getChannel().getName()) {
                            case "SMS":
                                success = sendSmsNotification(notification);
                                break;
                            case "EMAIL":
                                success = sendEmailNotification(notification);
                                break;
                            case "FCM":
                            case "PUSH":
                                success = sendPushNotification(notification);
                                break;
                            default:
                                log.warn("Unknown notification channel: {}", notification.getChannel().getName());
                                break;
                        }
                    }
                    
                    if (success) {
                        notification.setStatus("SENT");
                        notification.setUpdatedAt(LocalDateTime.now());
                        notificationQueueRepository.save(notification);
                        log.info("Successfully processed notification ID: {}", notification.getId());
                    } else {
                        // Update retry count and next retry time
                        notification.setRetryCount(notification.getRetryCount() + 1);
                        
                        if (notification.getRetryCount() >= 3) {
                            notification.setStatus("FAILED");
                            log.warn("Notification ID: {} failed after {} retries", notification.getId(), notification.getRetryCount());
                        } else {
                            int delayMinutes = (int) Math.pow(2, notification.getRetryCount()) * 5; // Exponential backoff
                            notification.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
                            log.info("Notification ID: {} will be retried in {} minutes", notification.getId(), delayMinutes);
                        }
                        
                        notification.setUpdatedAt(LocalDateTime.now());
                        notificationQueueRepository.save(notification);
                    }
                } catch (Exception e) {
                    log.error("Error processing notification ID: {}", notification.getId(), e);
                    
                    // Mark as ERROR
                    notification.setStatus("ERROR");
                    notification.setUpdatedAt(LocalDateTime.now());
                    notification.setErrorMessage(e.getMessage());
                    notificationQueueRepository.save(notification);
                }
            }
        } catch (Exception e) {
            log.error("Error in notification processing job", e);
        }
        
        log.info("Notification processing job completed");
    }
    
    /**
     * Send SMS notification
     * 
     * @param notification The notification to send
     * @return true if successful, false otherwise
     */
    private boolean sendSmsNotification(NotificationQueue notification) {
        try {
            String phoneNumber = null;
            
            // Check if this is a non-wallet recipient (referenceType starts with "PHONE:")
            if (notification.getReferenceType() != null && notification.getReferenceType().startsWith("PHONE:")) {
                // Extract phone number from referenceType
                phoneNumber = notification.getReferenceType().substring("PHONE:".length());
                log.info("Processing SMS for non-wallet recipient with phone number: {}", phoneNumber);
            } else {
                // Regular user, get phone number from user record
                try {
                    User user = userRepository.findById(notification.getUserId().longValue())
                        .orElse(null);
                    
                    if (user == null) {
                        log.warn("User not found for notification ID: {}, userId: {}", notification.getId(), notification.getUserId());
                        notification.setStatus("ERROR");
                        notification.setErrorMessage("User not found");
                        notificationQueueRepository.save(notification);
                        return false;
                    }
                    
                    phoneNumber = user.getPhoneNumber();
                } catch (Exception e) {
                    log.error("Error finding user for notification ID: {}, userId: {}", notification.getId(), notification.getUserId(), e);
                    notification.setStatus("ERROR");
                    notification.setErrorMessage("Error finding user: " + e.getMessage());
                    notificationQueueRepository.save(notification);
                    return false;
                }
            }
            
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                log.warn("No phone number available for notification ID: {}, cannot send SMS", notification.getId());
                return false;
            }
            
            // Use the SmsService to send the actual SMS
            log.info("Sending SMS to {}: {}", phoneNumber, notification.getContent());
            try {
                boolean smsSent = smsService.sendSms(phoneNumber, notification.getContent());
                if (!smsSent) {
                    log.error("SMS service failed to send message");
                    // Mark for retry
                    notification.setRetryCount(notification.getRetryCount() + 1);
                    notification.setNextRetryAt(LocalDateTime.now().plusMinutes(5 * notification.getRetryCount()));
                    notification.setStatus("PENDING");
                    return false;
                }
                return true;
            } catch (Exception e) {
                log.error("SMS service error: {}", e.getMessage());
                // Mark for retry
                notification.setRetryCount(notification.getRetryCount() + 1);
                notification.setNextRetryAt(LocalDateTime.now().plusMinutes(5 * notification.getRetryCount()));
                notification.setStatus("PENDING");
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send SMS notification ID: {}", notification.getId(), e);
            return false;
        }
    }
    
    /**
     * Send email notification
     * 
     * @param notification The notification to send
     * @return true if successful, false otherwise
     */
    private boolean sendEmailNotification(NotificationQueue notification) {
        try {
            // Get user's email
            User user = userRepository.findById(notification.getUserId().longValue())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            String email = user.getEmail();
            if (email == null || email.isEmpty()) {
                log.warn("User ID: {} has no email, cannot send email notification", notification.getUserId());
                return false;
            }
            
            // TODO: Implement actual email sending logic
            // This is a placeholder for demonstration
            log.info("Sending email to {}: Subject: {}, Content: {}", 
                    email, notification.getSubject(), notification.getContent());
            
            // Simulate successful sending
            return true;
        } catch (Exception e) {
            log.error("Failed to send email notification ID: {}", notification.getId(), e);
            return false;
        }
    }
    
    /**
     * Send push notification
     * 
     * @param notification The notification to send
     * @return true if successful, false otherwise
     */
    private boolean sendPushNotification(NotificationQueue notification) {
        try {
            log.info("Preparing to send push notification ID: {}", notification.getId());
            
            // Check if Firebase is available
            if (firebaseMessaging == null) {
                log.error("Firebase Messaging is not available, cannot send push notification.");
                log.error("Please check your Firebase configuration and make sure firebase-service-account.json exists.");
                
                // Don't retry if Firebase is not available - this is a configuration issue
                notification.setStatus("FAILED");
                notification.setErrorMessage("Firebase Messaging is not available - configuration issue");
                notificationQueueRepository.save(notification);
                return false;
            }
            
            // Get user's FCM tokens
            List<FcmToken> tokens = fcmTokenRepository.findAllByUserIdAndActiveTrue(notification.getUserId().longValue());
            
            log.info("Found {} active FCM tokens for user ID: {}", tokens.size(), notification.getUserId());
            
            if (tokens.isEmpty()) {
                log.warn("User ID: {} has no active FCM tokens, cannot send push notification", notification.getUserId());
                // Don't retry if user has no tokens
                notification.setStatus("FAILED");
                notification.setErrorMessage("User has no active FCM tokens");
                notificationQueueRepository.save(notification);
                return false;
            }
            
            boolean anySuccess = false;
            
            for (FcmToken token : tokens) {
                try {
                    log.info("Preparing FCM message for token: {}", token.getToken());
                    
                    // Prepare FCM message data
                    Map<String, String> data = new HashMap<>();
                    data.put("type", notification.getReferenceType() != null ? notification.getReferenceType() : "GENERAL");
                    
                    if (notification.getTransactionId() != null) {
                        data.put("transactionId", notification.getTransactionId().toString());
                    }
                    
                    // Create and send FCM message
                    Message message = Message.builder()
                        .setToken(token.getToken())
                        .setNotification(Notification.builder()
                            .setTitle(notification.getSubject())
                            .setBody(notification.getContent())
                            .build())
                        .putAllData(data)
                        .build();
                    
                    log.info("Sending FCM message to token: {}", token.getToken());
                    
                    String response = firebaseMessaging.send(message);
                    log.info("Successfully sent FCM notification to token {}: {}", token.getToken(), response);
                    anySuccess = true;
                } catch (FirebaseMessagingException e) {
                    log.error("Failed to send FCM notification to token: {}, Error: {}", token.getToken(), e.getMessage());
                    
                    // If token is invalid, mark it as inactive
                    if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                        e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                        log.info("Marking invalid token as inactive: {}", token.getToken());
                        token.setActive(false);
                        fcmTokenRepository.save(token);
                    }
                }
            }
            
            return anySuccess;
        } catch (Exception e) {
            log.error("Failed to send push notification ID: {}", notification.getId(), e);
            return false;
        }
    }

    /**
     * Process a notification immediately without waiting for the scheduled job
     * 
     * @param notificationId The ID of the notification to process
     * @return true if successful, false otherwise
     */
    @Transactional
    public boolean processNotificationImmediately(Integer notificationId) {
        log.info("Processing notification immediately: {}", notificationId);
        
        try {
            NotificationQueue notification = notificationQueueRepository.findById(notificationId)
                .orElse(null);
                
            if (notification == null) {
                log.warn("Notification not found: {}", notificationId);
                return false;
            }
            
            boolean success = false;
            
            // If channel object is null but channel ID is valid, try to determine channel type from ID
            if (notification.getChannel() == null) {
                Integer channelId = notification.getChannelId();
                log.info("Channel object is null but channel ID is: {}", channelId);
                
                if (channelId != null) {
                    // Map channel ID to type
                    String channelType = null;
                    switch (channelId) {
                        case 2:
                            channelType = "FCM";
                            break;
                        case 3:
                            channelType = "SMS";
                            break;
                        case 4:
                            channelType = "PUSH";
                            break;
                        case 5:
                            channelType = "EMAIL";
                            break;
                        case 6:
                            channelType = "IN-APP";
                            break;
                        default:
                            log.error("Unknown channel ID: {}", channelId);
                    }
                    
                    if (channelType != null) {
                        log.info("Determined channel type '{}' from ID: {}", channelType, channelId);
                        
                        switch (channelType) {
                            case "SMS":
                                success = sendSmsNotification(notification);
                                break;
                            case "EMAIL":
                                success = sendEmailNotification(notification);
                                break;
                            case "FCM":
                            case "PUSH":
                                success = sendPushNotification(notification);
                                break;
                            default:
                                log.warn("Unsupported channel type: {}", channelType);
                                break;
                        }
                    } else {
                        log.error("Could not determine channel type for ID: {}", channelId);
                        notification.setStatus("ERROR");
                        notification.setErrorMessage("Unknown channel ID: " + channelId);
                        notificationQueueRepository.save(notification);
                        return false;
                    }
                } else {
                    log.error("Channel is null for notification ID: {}", notification.getId());
                    notification.setStatus("ERROR");
                    notification.setErrorMessage("Channel is null");
                    notificationQueueRepository.save(notification);
                    return false;
                }
            } else {
                // Normal processing with channel object
                switch (notification.getChannel().getName()) {
                    case "SMS":
                        success = sendSmsNotification(notification);
                        break;
                    case "EMAIL":
                        success = sendEmailNotification(notification);
                        break;
                    case "FCM":
                    case "PUSH":
                        success = sendPushNotification(notification);
                        break;
                    default:
                        log.warn("Unknown notification channel: {}", notification.getChannel().getName());
                        break;
                }
            }
            
            if (success) {
                notification.setStatus("SENT");
                notification.setUpdatedAt(LocalDateTime.now());
                notificationQueueRepository.save(notification);
                log.info("Successfully processed notification ID: {}", notification.getId());
                return true;
            } else {
                // Update retry count and next retry time
                notification.setRetryCount(notification.getRetryCount() + 1);
                
                if (notification.getRetryCount() >= 3) {
                    notification.setStatus("FAILED");
                    log.warn("Notification ID: {} failed after {} retries", notification.getId(), notification.getRetryCount());
                } else {
                    int delayMinutes = (int) Math.pow(2, notification.getRetryCount()) * 5; // Exponential backoff
                    notification.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
                    log.info("Notification ID: {} will be retried in {} minutes", notification.getId(), delayMinutes);
                }
                
                notification.setUpdatedAt(LocalDateTime.now());
                notificationQueueRepository.save(notification);
                return false;
            }
        } catch (Exception e) {
            log.error("Error processing notification ID: {}", notificationId, e);
            return false;
        }
    }

    /**
     * Process all pending notifications immediately
     * This can be called after a transaction to ensure notifications are sent right away
     */
    @Transactional
    public void processAllPendingNotificationsNow() {
        log.info("Processing all pending notifications immediately");
        
        try {
            // Find all pending notifications regardless of nextRetryAt
            List<NotificationQueue> pendingNotifications = notificationQueueRepository.findByStatus("PENDING");
            
            log.info("Found {} pending notifications to process immediately", pendingNotifications.size());
            
            for (NotificationQueue notification : pendingNotifications) {
                try {
                    log.info("Processing notification ID: {}, type: {}, channel: {}, user: {}", 
                        notification.getId(), 
                        notification.getNotificationType() != null ? notification.getNotificationType().getName() : "unknown",
                        notification.getChannel() != null ? notification.getChannel().getName() : "unknown",
                        notification.getUserId());
                    
                    // Process notification based on channel
                    boolean success = false;
                    
                    // If channel object is null but channel ID is valid, try to determine channel type from ID
                    if (notification.getChannel() == null) {
                        Integer channelId = notification.getChannelId();
                        log.info("Channel object is null but channel ID is: {}", channelId);
                        
                        if (channelId != null) {
                            // Map channel ID to type
                            String channelType = null;
                            switch (channelId) {
                                case 2:
                                    channelType = "FCM";
                                    break;
                                case 3:
                                    channelType = "SMS";
                                    break;
                                case 4:
                                    channelType = "PUSH";
                                    break;
                                case 5:
                                    channelType = "EMAIL";
                                    break;
                                case 6:
                                    channelType = "IN-APP";
                                    break;
                                default:
                                    log.error("Unknown channel ID: {}", channelId);
                            }
                            
                            if (channelType != null) {
                                log.info("Determined channel type '{}' from ID: {}", channelType, channelId);
                                
                                switch (channelType) {
                                    case "SMS":
                                        success = sendSmsNotification(notification);
                                        break;
                                    case "EMAIL":
                                        success = sendEmailNotification(notification);
                                        break;
                                    case "FCM":
                                    case "PUSH":
                                        success = sendPushNotification(notification);
                                        break;
                                    default:
                                        log.warn("Unsupported channel type: {}", channelType);
                                        break;
                                }
                            } else {
                                log.error("Could not determine channel type for ID: {}", channelId);
                                notification.setStatus("ERROR");
                                notification.setErrorMessage("Unknown channel ID: " + channelId);
                                notificationQueueRepository.save(notification);
                                continue;
                            }
                        } else {
                            log.error("Channel is null for notification ID: {}", notification.getId());
                            notification.setStatus("ERROR");
                            notification.setErrorMessage("Channel is null");
                            notificationQueueRepository.save(notification);
                            continue;
                        }
                    } else {
                        // Normal processing with channel object
                        switch (notification.getChannel().getName()) {
                            case "SMS":
                                success = sendSmsNotification(notification);
                                break;
                            case "EMAIL":
                                success = sendEmailNotification(notification);
                                break;
                            case "FCM":
                            case "PUSH":
                                success = sendPushNotification(notification);
                                break;
                            default:
                                log.warn("Unknown notification channel: {}", notification.getChannel().getName());
                                break;
                        }
                    }
                    
                    if (success) {
                        notification.setStatus("SENT");
                        notification.setUpdatedAt(LocalDateTime.now());
                        notificationQueueRepository.save(notification);
                        log.info("Successfully processed notification ID: {}", notification.getId());
                    } else {
                        // Update retry count and next retry time
                        notification.setRetryCount(notification.getRetryCount() + 1);
                        
                        if (notification.getRetryCount() >= 3) {
                            notification.setStatus("FAILED");
                            log.warn("Notification ID: {} failed after {} retries", notification.getId(), notification.getRetryCount());
                        } else {
                            int delayMinutes = (int) Math.pow(2, notification.getRetryCount()) * 5; // Exponential backoff
                            notification.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
                            log.info("Notification ID: {} will be retried in {} minutes", notification.getId(), delayMinutes);
                        }
                        
                        notification.setUpdatedAt(LocalDateTime.now());
                        notificationQueueRepository.save(notification);
                    }
                } catch (Exception e) {
                    log.error("Error processing notification ID: {}", notification.getId(), e);
                    
                    // Mark as ERROR
                    notification.setStatus("ERROR");
                    notification.setUpdatedAt(LocalDateTime.now());
                    notification.setErrorMessage(e.getMessage());
                    notificationQueueRepository.save(notification);
                }
            }
        } catch (Exception e) {
            log.error("Error in immediate notification processing", e);
        }
        
        log.info("Immediate notification processing completed");
    }
} 
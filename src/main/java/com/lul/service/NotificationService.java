package com.lul.service;

import com.lul.entity.Channel;
import com.lul.entity.NotificationQueue;
import com.lul.entity.NotificationType;
import com.lul.entity.TransactionHistory;
import com.lul.entity.User;
import com.lul.entity.FcmToken;
import com.lul.constant.ErrorCode;
import com.lul.exception.NotFoundException;
import com.lul.repository.ChannelRepository;
import com.lul.repository.FcmTokenRepository;
import com.lul.repository.NotificationQueueRepository;
import com.lul.repository.NotificationTypeRepository;
import com.lul.repository.TransactionHistoryRepository;
import com.lul.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;

/**
 * Service for handling notifications
 */
@Service
@Slf4j
public class NotificationService {
    
    private final NotificationQueueRepository notificationQueueRepository;
    private final NotificationTypeRepository notificationTypeRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final NotificationProcessor notificationProcessor;
    private final FcmTokenRepository fcmTokenRepository;
    private final SmsService smsService;
    private final FirebaseMessaging firebaseMessaging;
    
    @Autowired
    public NotificationService(
            NotificationQueueRepository notificationQueueRepository,
            NotificationTypeRepository notificationTypeRepository,
            ChannelRepository channelRepository,
            UserRepository userRepository,
            TransactionHistoryRepository transactionHistoryRepository,
            NotificationProcessor notificationProcessor,
            FcmTokenRepository fcmTokenRepository,
            SmsService smsService,
            @Autowired(required = false) FirebaseMessaging firebaseMessaging) {
        this.notificationQueueRepository = notificationQueueRepository;
        this.notificationTypeRepository = notificationTypeRepository;
        this.channelRepository = channelRepository;
        this.userRepository = userRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.notificationProcessor = notificationProcessor;
        this.fcmTokenRepository = fcmTokenRepository;
        this.smsService = smsService;
        this.firebaseMessaging = firebaseMessaging;
        
        if (this.firebaseMessaging == null) {
            log.warn("FirebaseMessaging is not available. FCM notifications will not be sent.");
        }
    }

    /**
     * Queue a transaction notification for a user
     * 
     * @param userId The user ID
     * @param notificationTypeName The notification type name
     * @param subject The notification subject
     * @param content The notification content
     * @param transactionId The transaction ID
     */
    public void queueTransactionNotification(Integer userId, String notificationTypeName, String subject, String content, Long transactionId) {
        try {
            // Get notification type
            NotificationType notificationType = notificationTypeRepository.findByName(notificationTypeName)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND));
            
            // Try to queue push notification
            Integer pushChannelId = getChannelId("PUSH");
            if (pushChannelId != null) {
                queueNotification(userId, notificationType.getId(), pushChannelId, subject, content, transactionId, null, null);
            } else {
                log.warn("PUSH channel not found, trying FCM instead");
                Integer fcmChannelId = getChannelId("FCM");
                if (fcmChannelId != null) {
                    queueNotification(userId, notificationType.getId(), fcmChannelId, subject, content, transactionId, null, null);
                } else {
                    log.warn("No push notification channel found (PUSH or FCM)");
                }
            }
            
            // For received transactions, also try to send SMS
            if ("TRANSACTION_RECEIVED".equals(notificationTypeName)) {
                Integer smsChannelId = getChannelId("SMS");
                if (smsChannelId != null) {
                    queueNotification(userId, notificationType.getId(), smsChannelId, subject, content, transactionId, null, null);
                } else {
                    log.warn("SMS channel not found, skipping SMS notification");
                }
            }
        } catch (Exception e) {
            log.error("Failed to queue notification for user ID: {}, transaction ID: {}", userId, transactionId, e);
        }
    }
    
    /**
     * Queue a notification
     * 
     * @param userId The user ID
     * @param notificationTypeId The notification type ID
     * @param channelId The channel ID
     * @param subject The notification subject
     * @param content The notification content
     * @param transactionId The transaction ID (optional)
     * @param referenceId The reference ID (optional)
     * @param referenceType The reference type (optional)
     */
    private void queueNotification(Integer userId, Integer notificationTypeId, Integer channelId, 
                                  String subject, String content, Long transactionId, 
                                  Long referenceId, String referenceType) {
        // Skip if channel ID is null (channel not found)
        if (channelId == null) {
            log.warn("Skipping notification for user ID: {} as channel ID is null", userId);
            return;
        }
        
        try {
            // Verify that the channel ID exists in the database
            boolean channelExists = channelRepository.existsById(channelId);
            if (!channelExists) {
                log.error("Channel ID {} does not exist in the database. Available channels: {}", 
                    channelId, 
                    channelRepository.findAll().stream()
                        .map(c -> c.getId() + ":" + c.getName())
                        .collect(Collectors.joining(", ")));
                return;
            }
            
            NotificationQueue notification = new NotificationQueue();
            notification.setUserId(userId);
            notification.setNotificationTypeId(notificationTypeId);
            notification.setChannelId(channelId);
            notification.setSubject(subject);
            notification.setContent(content);
            notification.setTransactionId(transactionId);
            notification.setReferenceId(referenceId);
            notification.setReferenceType(referenceType);
            notification.setStatus("PENDING");
            notification.setRetryCount(0);
            // Set to current time so it's processed immediately
            notification.setNextRetryAt(LocalDateTime.now());
            
            notificationQueueRepository.save(notification);
            log.info("Successfully queued notification ID: {} for user ID: {}, channel ID: {}, transaction ID: {}", 
                    notification.getId(), userId, channelId, transactionId);
        } catch (Exception e) {
            log.error("Failed to queue notification for user ID: {}, channel ID: {}, error: {}", 
                userId, channelId, e.getMessage(), e);
        }
    }
    
    /**
     * Get channel ID by name
     * 
     * @param channelName The channel name
     * @return The channel ID or null if not found
     */
    private Integer getChannelId(String channelName) {
        try {
            log.info("Looking for channel with name: {}", channelName);
            
            // List all available channels for debugging
            List<Channel> allChannels = channelRepository.findAll();
            log.info("Available channels: {}", allChannels.stream()
                .map(c -> c.getId() + ":" + c.getName())
                .collect(Collectors.joining(", ")));
            
            // Map the input channel name to the correct channel ID based on the database
            // Channel IDs in the database: 2=FCM, 3=SMS, 4=Push, 5=EMAIL, 6=In-APP
            Integer channelId = null;
            
            switch (channelName.toUpperCase()) {
                case "FCM":
                    channelId = 2;
                    break;
                case "SMS":
                    channelId = 3;
                    break;
                case "PUSH":
                    channelId = 4;
                    break;
                case "EMAIL":
                    channelId = 5;
                    break;
                case "IN-APP":
                    channelId = 6;
                    break;
                default:
                    // Try to find by name as a fallback
                    Channel channel = channelRepository.findByName(channelName)
                        .orElse(null);
                    
                    if (channel != null) {
                        channelId = channel.getId();
                    }
            }
            
            if (channelId != null) {
                log.info("Mapped channel name '{}' to ID: {}", channelName, channelId);
                return channelId;
            }
            
            log.warn("No channel ID found for name: {}", channelName);
            throw new NotFoundException(ErrorCode.NOTIFICATION_CHANNEL_NOT_FOUND);
        } catch (NotFoundException e) {
            log.warn("Notification channel not found: {}. This may be expected if the channel is not configured.", channelName);
            return null;
        }
    }

    /**
     * Send a notification to a user
     * 
     * @param userId The user ID
     * @param title The notification title
     * @param body The notification body
     * @param data Additional data to include in the notification
     */
    public void sendNotification(Long userId, String title, String body, Map<String, String> data) {
        log.info("Sending notification to user ID: {}, title: {}", userId, title);
        
        try {
            // Get notification type for general notifications
            NotificationType notificationType = notificationTypeRepository.findByName("GENERAL")
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND));
            
            // Try FCM first, then fall back to PUSH
            Integer channelId = getChannelId("FCM");
            if (channelId == null) {
                log.warn("FCM channel not found, trying PUSH channel instead");
                channelId = getChannelId("PUSH");
                
                if (channelId == null) {
                    log.warn("No suitable push notification channel found. Skipping notification.");
                    return;
                }
            }
            
            // Queue notification
            queueNotification(
                userId.intValue(),
                notificationType.getId(),
                channelId,
                title,
                body,
                null,
                null,
                null
            );
            
            // Also send FCM notification directly if available
            if (firebaseMessaging != null) {
                // Get ALL active tokens for this user
                List<FcmToken> tokens = fcmTokenRepository.findAllByUserIdAndActiveTrue(userId);
                
                if (tokens.isEmpty()) {
                    log.warn("No active FCM tokens found for user: {}. FCM notification will not be sent.", userId);
                    return;
                }
                
                for (FcmToken token : tokens) {
                    try {
                        Message message = Message.builder()
                            .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                            .putAllData(data)
                            .setToken(token.getToken())
                            .build();
                        
                        String response = firebaseMessaging.send(message);
                        log.info("Successfully sent FCM notification to device: {}, response: {}", 
                            token.getDeviceId() != null ? token.getDeviceId() : "unknown", 
                            response);
                    } catch (FirebaseMessagingException e) {
                        log.error("Failed to send notification to token: {}, Error code: {}, Message: {}", 
                            token.getToken(), e.getMessagingErrorCode(), e.getMessage());
                        
                        // If the token is invalid or unregistered, mark it as inactive
                        if (e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT || 
                            e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                            log.info("Marking invalid token as inactive: {}", token.getToken());
                            token.setActive(false);
                            fcmTokenRepository.save(token);
                        }
                    }
                }
            } else {
                log.warn("FirebaseMessaging is not available. FCM notification will not be sent directly.");
            }
        } catch (Exception e) {
            log.error("Failed to send notification to user ID: {}", userId, e);
        }
    }
    
    /**
     * Send a transaction notification to the sender
     * 
     * @param userId The sender's user ID
     * @param title The notification title
     * @param fcmBody The FCM notification body
     * @param transactionId The transaction ID
     * @param smsBody The SMS notification body (if null, fcmBody will be used)
     */
    public void sendTransactionNotificationToSender(Integer userId, String title, String fcmBody, Long transactionId, String smsBody) {
        log.info("Sending transaction notification to sender ID: {}, transaction ID: {}", userId, transactionId);
        
        try {
            // Get notification type for transaction sent
            NotificationType notificationType = notificationTypeRepository.findByName("TRANSACTION_SENT")
                .orElse(null);
            
            if (notificationType == null) {
                log.warn("Notification type TRANSACTION_SENT not found. Skipping notification.");
                return;
            }
            
            // Try to find FCM channel first, then fall back to PUSH if available
            Integer pushChannelId = getChannelId("FCM");
            if (pushChannelId == null) {
                log.warn("FCM channel not found, trying PUSH channel instead");
                pushChannelId = getChannelId("PUSH");
                
                if (pushChannelId == null) {
                    log.warn("No suitable push notification channel found. Skipping push notification.");
                } else {
                    // Queue push notification
                    queueNotification(
                        userId,
                        notificationType.getId(),
                        pushChannelId,
                        title,
                        fcmBody,
                        transactionId,
                        null,
                        "TRANSACTION"
                    );
                    log.info("Push notification queued for sender ID: {}", userId);
                }
            } else {
                // Queue push notification
                queueNotification(
                    userId,
                    notificationType.getId(),
                    pushChannelId,
                    title,
                    fcmBody,
                    transactionId,
                    null,
                    "TRANSACTION"
                );
                log.info("FCM notification queued for sender ID: {}", userId);
            }
            
            // Also send SMS notification to sender
            Integer smsChannelId = getChannelId("SMS");
            if (smsChannelId != null) {
                // Queue SMS notification with specific SMS message if provided
                queueNotification(
                    userId,
                    notificationType.getId(),
                    smsChannelId,
                    title,
                    smsBody != null ? smsBody : fcmBody,  // Use SMS-specific message if provided
                    transactionId,
                    null,
                    "TRANSACTION"
                );
                log.info("SMS notification queued for sender ID: {}", userId);
            } else {
                log.warn("SMS channel not found. Skipping SMS notification for sender.");
            }
            
            log.info("Transaction notifications queued for sender ID: {}", userId);
        } catch (Exception e) {
            log.error("Failed to queue transaction notifications for sender ID: {}", userId, e);
        }
    }
    
    /**
     * Send a transaction notification to the sender (backward compatibility)
     */
    public void sendTransactionNotificationToSender(Integer userId, String title, String body, Long transactionId) {
        sendTransactionNotificationToSender(userId, title, body, transactionId, body);
    }
    
    /**
     * Send transaction notification to receiver
     * 
     * @param userId The receiver's user ID
     * @param title The notification title
     * @param fcmBody The FCM notification body
     * @param transactionId The transaction ID
     * @param smsBody The SMS notification body (if null, fcmBody will be used)
     */
    public void sendTransactionNotificationToReceiver(
            Integer userId, 
            String title, 
            String fcmBody, 
            Long transactionId,
            String smsBody) {
        
        try {
            // Get notification type for transaction received
            NotificationType notificationType = notificationTypeRepository.findByName("TRANSACTION_RECEIVED")
                .orElse(null);
                
            if (notificationType == null) {
                log.warn("Notification type TRANSACTION_RECEIVED not found. Skipping notification.");
                return;
            }
            
            // Try to find FCM channel first, then fall back to PUSH if available
            Integer pushChannelId = getChannelId("FCM");
            if (pushChannelId == null) {
                log.warn("FCM channel not found, trying PUSH channel instead");
                pushChannelId = getChannelId("PUSH");
                
                if (pushChannelId == null) {
                    log.warn("No suitable push notification channel found. Skipping push notification.");
                } else {
                    // Queue push notification with FCM message
                    queueNotification(
                        userId,
                        notificationType.getId(),
                        pushChannelId,
                        title,
                        fcmBody,
                        transactionId,
                        null,
                        "TRANSACTION"
                    );
                }
            } else {
                // Queue push notification with FCM message
                queueNotification(
                    userId,
                    notificationType.getId(),
                    pushChannelId,
                    title,
                    fcmBody,
                    transactionId,
                    null,
                    "TRANSACTION"
                );
            }
            
            // Try to find SMS channel
            Integer smsChannelId = getChannelId("SMS");
            if (smsChannelId == null) {
                log.warn("SMS channel not found. Skipping SMS notification.");
            } else {
                // Queue SMS notification with SMS-specific message if provided
                queueNotification(
                    userId,
                    notificationType.getId(),
                    smsChannelId,
                    title,
                    smsBody != null ? smsBody : fcmBody,
                    transactionId,
                    null,
                    "TRANSACTION"
                );
            }
            
            log.info("Transaction notifications queued for receiver ID: {}", userId);
        } catch (Exception e) {
            log.error("Failed to queue transaction notification for receiver ID: {}", userId, e);
        }
    }
    
    /**
     * Send transaction notification to receiver (backward compatibility)
     */
    public void sendTransactionNotificationToReceiver(Integer userId, String title, String body, Long transactionId) {
        sendTransactionNotificationToReceiver(userId, title, body, transactionId, body);
    }
    
    /**
     * Queue an SMS notification for a non-wallet recipient
     * 
     * @param tempUserId A temporary user ID for the non-wallet recipient (negative to avoid conflicts)
     * @param phoneNumber The recipient's phone number
     * @param subject The notification subject
     * @param content The notification content
     * @param transactionId The transaction ID
     * @deprecated This method is deprecated. Use direct SMS sending for non-wallet recipients instead.
     */
    @Deprecated
    public void queueSmsNotification(Integer tempUserId, String phoneNumber, String subject, String content, Long transactionId) {
        log.info("Direct SMS sending is now preferred for non-wallet recipients. Sending SMS directly.");
        
        try {
            // Send SMS directly instead of queuing
            smsService.sendSms(phoneNumber, content);
            log.info("Successfully sent SMS directly to non-wallet recipient: {}, transaction ID: {}", 
                    phoneNumber, transactionId);
        } catch (Exception e) {
            log.error("Failed to send SMS to non-wallet recipient: {}, error: {}", 
                phoneNumber, e.getMessage(), e);
            // Don't re-throw to prevent transaction failure
        }
    }
    
    /**
     * Send an FCM notification to the sender (without SMS)
     * 
     * @param userId The sender's user ID
     * @param title The notification title
     * @param fcmBody The FCM notification body
     * @param transactionId The transaction ID
     */
    public void sendFcmNotificationToSender(Integer userId, String title, String fcmBody, Long transactionId) {
        log.info("Sending FCM-only notification to sender ID: {}, transaction ID: {}", userId, transactionId);
        
        try {
            // Get notification type for transaction sent
            NotificationType notificationType = notificationTypeRepository.findByName("TRANSACTION_SENT")
                .orElse(null);
            
            if (notificationType == null) {
                log.warn("Notification type TRANSACTION_SENT not found. Skipping notification.");
                return;
            }
            
            // Try to find FCM channel first, then fall back to PUSH if available
            Integer pushChannelId = getChannelId("FCM");
            if (pushChannelId == null) {
                log.warn("FCM channel not found, trying PUSH channel instead");
                pushChannelId = getChannelId("PUSH");
                
                if (pushChannelId == null) {
                    log.warn("No suitable push notification channel found. Skipping push notification.");
                } else {
                    // Queue push notification
                    queueNotification(
                        userId,
                        notificationType.getId(),
                        pushChannelId,
                        title,
                        fcmBody,
                        transactionId,
                        null,
                        "TRANSACTION"
                    );
                    log.info("Push notification queued for sender ID: {}", userId);
                }
            } else {
                // Queue push notification
                queueNotification(
                    userId,
                    notificationType.getId(),
                    pushChannelId,
                    title,
                    fcmBody,
                    transactionId,
                    null,
                    "TRANSACTION"
                );
                log.info("FCM notification queued for sender ID: {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to queue FCM notification for sender ID: {}", userId, e);
        }
    }
    
    /**
     * Send an SMS directly without queuing
     * 
     * @param phoneNumber The phone number to send the SMS to
     * @param message The SMS message
     * @return true if the SMS was sent successfully, false otherwise
     */
    public boolean sendDirectSms(String phoneNumber, String message) {
        try {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                log.warn("Cannot send SMS to null or empty phone number");
                return false;
            }
            
            if (message == null || message.isEmpty()) {
                log.warn("Cannot send empty SMS message");
                return false;
            }
            
            return smsService.sendSms(phoneNumber, message);
        } catch (Exception e) {
            log.error("Failed to send direct SMS to {}: {}", phoneNumber, e.getMessage(), e);
            return false;
        }
    }
} 
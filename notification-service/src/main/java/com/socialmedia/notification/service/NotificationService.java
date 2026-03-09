package com.socialmedia.notification.service;

import com.socialmedia.notification.model.Notification;
import com.socialmedia.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;

    @Transactional
    public Notification createNotification(String userId, String message,
                                           Notification.NotificationType type, Map<String, Object> metadata) {
        Notification notification = Notification.builder()
                .userId(userId)
                .message(message)
                .type(type)
                .metadata(metadata.toString())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);

        // Push real-time notification via WebSocket
        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/notifications",
                notification
        );

        log.info("Notification sent to user {}: {}", userId, message);
        return notification;
    }

    public void notifyFollowers(String authorId, String message,
                                 Notification.NotificationType type, Map<String, Object> metadata) {
        try {
            String[] followerIds = restTemplate.getForObject(
                    "http://user-service/api/users/internal/followers/" + authorId,
                    String[].class
            );

            if (followerIds != null) {
                Arrays.stream(followerIds).forEach(followerId ->
                        createNotification(followerId, message, type, metadata)
                );
                log.info("Notified {} followers of {}", followerIds.length, authorId);
            }
        } catch (Exception e) {
            log.error("Failed to notify followers of {}: {}", authorId, e.getMessage());
        }
    }

    public Page<Notification> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(String notificationId, String userId) {
        notificationRepository.findByIdAndUserId(notificationId, userId)
                .ifPresent(notification -> {
                    notification.setRead(true);
                    notification.setReadAt(LocalDateTime.now());
                    notificationRepository.save(notification);
                });
    }

    @Transactional
    public void markAllAsRead(String userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(userId);
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(n -> {
            n.setRead(true);
            n.setReadAt(now);
        });
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void deleteNotification(String notificationId, String userId) {
        notificationRepository.deleteByIdAndUserId(notificationId, userId);
    }
}

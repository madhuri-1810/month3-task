package com.socialmedia.notification.consumer;

import com.socialmedia.notification.model.Notification;
import com.socialmedia.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    /**
     * Handles post.created events → notify all followers
     */
    @RabbitListener(queues = "post.created.queue")
    public void handlePostCreated(Map<String, Object> event) {
        try {
            String authorId = (String) event.get("authorId");
            String authorUsername = (String) event.get("authorUsername");
            String postId = (String) event.get("postId");

            log.info("Processing post.created event for author: {}", authorId);

            notificationService.notifyFollowers(
                    authorId,
                    authorUsername + " created a new post",
                    Notification.NotificationType.NEW_POST,
                    Map.of("postId", postId, "authorId", authorId)
            );
        } catch (Exception e) {
            log.error("Error processing post.created event", e);
        }
    }

    /**
     * Handles user.followed events → notify followed user
     */
    @RabbitListener(queues = "user.followed.queue")
    public void handleUserFollowed(Map<String, Object> event) {
        try {
            String followerId = (String) event.get("followerId");
            String followerUsername = (String) event.get("followerUsername");
            String targetUserId = (String) event.get("targetUserId");

            log.info("Processing user.followed event: {} followed {}", followerId, targetUserId);

            notificationService.createNotification(
                    targetUserId,
                    followerUsername + " started following you",
                    Notification.NotificationType.NEW_FOLLOWER,
                    Map.of("followerId", followerId, "followerUsername", followerUsername)
            );
        } catch (Exception e) {
            log.error("Error processing user.followed event", e);
        }
    }

    /**
     * Handles post.liked events → notify post author
     */
    @RabbitListener(queues = "post.liked.queue")
    public void handlePostLiked(Map<String, Object> event) {
        try {
            String likerId = (String) event.get("likerId");
            String postId = (String) event.get("postId");
            String postAuthorId = (String) event.get("postAuthorId");
            String likerUsername = (String) event.get("likerUsername");

            if (!likerId.equals(postAuthorId)) {
                notificationService.createNotification(
                        postAuthorId,
                        likerUsername + " liked your post",
                        Notification.NotificationType.POST_LIKED,
                        Map.of("postId", postId, "likerId", likerId)
                );
            }
        } catch (Exception e) {
            log.error("Error processing post.liked event", e);
        }
    }

    /**
     * Handles message.sent events → notify chat recipient
     */
    @RabbitListener(queues = "message.sent.queue")
    public void handleMessageSent(Map<String, Object> event) {
        try {
            String senderId = (String) event.get("senderId");
            String senderUsername = (String) event.get("senderUsername");
            String recipientId = (String) event.get("recipientId");
            String messagePreview = (String) event.get("messagePreview");

            notificationService.createNotification(
                    recipientId,
                    senderUsername + ": " + messagePreview,
                    Notification.NotificationType.NEW_MESSAGE,
                    Map.of("senderId", senderId, "senderUsername", senderUsername)
            );
        } catch (Exception e) {
            log.error("Error processing message.sent event", e);
        }
    }
}

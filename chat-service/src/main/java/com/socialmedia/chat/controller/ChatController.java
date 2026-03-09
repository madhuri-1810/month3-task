package com.socialmedia.chat.controller;

import com.socialmedia.chat.dto.*;
import com.socialmedia.chat.model.Message;
import com.socialmedia.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    // ─── WebSocket endpoints ─────────────────────────────────────────
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {
        String senderId = (String) headerAccessor.getSessionAttributes().get("userId");
        Message message = chatService.sendMessage(senderId, request);

        // Deliver to recipient in real time
        messagingTemplate.convertAndSendToUser(
                request.getRecipientId(),
                "/queue/messages",
                message
        );

        // Echo back to sender
        messagingTemplate.convertAndSendToUser(
                senderId,
                "/queue/messages",
                message
        );
    }

    @MessageMapping("/chat.typing")
    public void typingIndicator(@Payload TypingIndicatorRequest request,
                                SimpMessageHeaderAccessor headerAccessor) {
        String senderId = (String) headerAccessor.getSessionAttributes().get("userId");
        messagingTemplate.convertAndSendToUser(
                request.getRecipientId(),
                "/queue/typing",
                new TypingIndicatorEvent(senderId, request.isTyping())
        );
    }

    @MessageMapping("/chat.read")
    public void markRead(@Payload ReadReceiptRequest request,
                         SimpMessageHeaderAccessor headerAccessor) {
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        chatService.markMessagesAsRead(userId, request.getSenderId());

        // Notify sender of read receipt
        messagingTemplate.convertAndSendToUser(
                request.getSenderId(),
                "/queue/read-receipts",
                new ReadReceiptEvent(userId, request.getConversationId())
        );
    }

    // ─── REST endpoints ──────────────────────────────────────────────
    @GetMapping("/api/chat/conversations")
    public ResponseEntity<Page<ConversationDTO>> getConversations(
            @RequestHeader("X-User-Id") String userId,
            Pageable pageable) {
        return ResponseEntity.ok(chatService.getUserConversations(userId, pageable));
    }

    @GetMapping("/api/chat/messages/{otherUserId}")
    public ResponseEntity<Page<MessageDTO>> getMessages(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String otherUserId,
            Pageable pageable) {
        return ResponseEntity.ok(chatService.getMessages(userId, otherUserId, pageable));
    }

    @DeleteMapping("/api/chat/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable String messageId,
            @RequestHeader("X-User-Id") String userId) {
        chatService.deleteMessage(messageId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/chat/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(chatService.getUnreadCount(userId));
    }
}

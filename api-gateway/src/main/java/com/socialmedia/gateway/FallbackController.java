package com.socialmedia.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user")
    public ResponseEntity<Map<String, String>> userFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "service", "user-service",
                        "status", "unavailable",
                        "message", "User Service is currently down. Please try again later."
                ));
    }

    @GetMapping("/post")
    public ResponseEntity<Map<String, String>> postFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "service", "post-service",
                        "status", "unavailable",
                        "message", "Post Service is currently down. Please try again later."
                ));
    }

    @GetMapping("/notification")
    public ResponseEntity<Map<String, String>> notificationFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "service", "notification-service",
                        "status", "unavailable",
                        "message", "Notification Service is temporarily unavailable."
                ));
    }

    @GetMapping("/chat")
    public ResponseEntity<Map<String, String>> chatFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "service", "chat-service",
                        "status", "unavailable",
                        "message", "Chat Service is currently down. Messages will be delayed."
                ));
    }

    @GetMapping("/media")
    public ResponseEntity<Map<String, String>> mediaFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "service", "media-service",
                        "status", "unavailable",
                        "message", "Media Service is currently down. Uploads are temporarily disabled."
                ));
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, String>> analyticsFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "service", "analytics-service",
                        "status", "unavailable",
                        "message", "Analytics Service is currently down."
                ));
    }
}

package com.socialmedia.user.controller;

import com.socialmedia.user.dto.*;
import com.socialmedia.user.service.UserService;
import com.socialmedia.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    // ─── Auth Endpoints ──────────────────────────────────────────────
    @PostMapping("/api/auth/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("X-Refresh-Token") String refreshToken) {
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(@RequestHeader("X-User-Id") String userId) {
        authService.logout(userId);
        return ResponseEntity.ok().build();
    }

    // ─── User Endpoints ──────────────────────────────────────────────
    @GetMapping("/api/users/{userId}")
    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @GetMapping("/api/users/username/{username}")
    public ResponseEntity<UserProfileDTO> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @PutMapping("/api/users/profile")
    public ResponseEntity<UserProfileDTO> updateProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @GetMapping("/api/users/search")
    public ResponseEntity<Page<UserSummaryDTO>> searchUsers(
            @RequestParam String query,
            Pageable pageable) {
        return ResponseEntity.ok(userService.searchUsers(query, pageable));
    }

    // ─── Follow Endpoints ────────────────────────────────────────────
    @PostMapping("/api/users/{targetUserId}/follow")
    public ResponseEntity<FollowResponse> followUser(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String targetUserId) {
        FollowResponse response = userService.followUser(userId, targetUserId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/users/{targetUserId}/follow")
    public ResponseEntity<FollowResponse> unfollowUser(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String targetUserId) {
        FollowResponse response = userService.unfollowUser(userId, targetUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/users/{userId}/followers")
    public ResponseEntity<Page<UserSummaryDTO>> getFollowers(
            @PathVariable String userId,
            Pageable pageable) {
        return ResponseEntity.ok(userService.getFollowers(userId, pageable));
    }

    @GetMapping("/api/users/{userId}/following")
    public ResponseEntity<Page<UserSummaryDTO>> getFollowing(
            @PathVariable String userId,
            Pageable pageable) {
        return ResponseEntity.ok(userService.getFollowing(userId, pageable));
    }

    // ─── Internal Endpoints (for other microservices) ────────────────
    @GetMapping("/api/users/internal/{userId}")
    public ResponseEntity<UserSummaryDTO> getUserSummary(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserSummary(userId));
    }

    @GetMapping("/api/users/internal/followers/{userId}")
    public ResponseEntity<java.util.List<String>> getFollowerIds(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getFollowerIds(userId));
    }
}

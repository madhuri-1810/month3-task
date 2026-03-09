package com.socialmedia.user.service;

import com.socialmedia.user.dto.*;
import com.socialmedia.user.model.User;
import com.socialmedia.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    public UserProfileDTO getUserProfile(String userId) {
        User user = findUserById(userId);
        return mapToProfile(user);
    }

    public UserProfileDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return mapToProfile(user);
    }

    @Transactional
    public UserProfileDTO updateProfile(String userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (request.getDisplayName() != null) user.setDisplayName(request.getDisplayName());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());
        if (request.getCoverImageUrl() != null) user.setCoverImageUrl(request.getCoverImageUrl());

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", userId);
        return mapToProfile(user);
    }

    public Page<UserSummaryDTO> searchUsers(String query, Pageable pageable) {
        return userRepository.searchByUsernameOrDisplayName(query, pageable)
                .map(this::mapToSummary);
    }

    @Transactional
    public FollowResponse followUser(String userId, String targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new RuntimeException("Cannot follow yourself");
        }

        User user = findUserById(userId);
        User targetUser = findUserById(targetUserId);

        if (targetUser.getFollowers().contains(user)) {
            throw new RuntimeException("Already following this user");
        }

        targetUser.getFollowers().add(user);
        userRepository.save(targetUser);

        // Publish event
        UserFollowedEvent event = new UserFollowedEvent(userId, targetUserId,
                user.getUsername(), targetUser.getUsername());
        rabbitTemplate.convertAndSend("social-media.exchange", "user.followed", event);

        log.info("User {} followed {}", userId, targetUserId);
        return new FollowResponse(true, targetUser.getFollowers().size());
    }

    @Transactional
    public FollowResponse unfollowUser(String userId, String targetUserId) {
        User user = findUserById(userId);
        User targetUser = findUserById(targetUserId);

        targetUser.getFollowers().remove(user);
        userRepository.save(targetUser);

        log.info("User {} unfollowed {}", userId, targetUserId);
        return new FollowResponse(false, targetUser.getFollowers().size());
    }

    public Page<UserSummaryDTO> getFollowers(String userId, Pageable pageable) {
        return userRepository.findFollowersByUserId(userId, pageable)
                .map(this::mapToSummary);
    }

    public Page<UserSummaryDTO> getFollowing(String userId, Pageable pageable) {
        return userRepository.findFollowingByUserId(userId, pageable)
                .map(this::mapToSummary);
    }

    public UserSummaryDTO getUserSummary(String userId) {
        return mapToSummary(findUserById(userId));
    }

    public List<String> getFollowerIds(String userId) {
        User user = findUserById(userId);
        return user.getFollowers().stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    private User findUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    private UserProfileDTO mapToProfile(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .bio(user.getBio())
                .profileImageUrl(user.getProfileImageUrl())
                .coverImageUrl(user.getCoverImageUrl())
                .role(user.getRole().name())
                .isVerified(user.isVerified())
                .followersCount(user.getFollowers().size())
                .followingCount(user.getFollowing().size())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserSummaryDTO mapToSummary(User user) {
        return UserSummaryDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .profileImageUrl(user.getProfileImageUrl())
                .isVerified(user.isVerified())
                .build();
    }
}

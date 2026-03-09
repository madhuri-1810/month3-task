package com.socialmedia.post.controller;

import com.socialmedia.post.dto.*;
import com.socialmedia.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/api/posts")
    public ResponseEntity<PostDTO> createPost(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Username") String username,
            @Valid @RequestBody CreatePostRequest request) {
        PostDTO post = postService.createPost(userId, username, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }

    @GetMapping("/api/posts/{postId}")
    public ResponseEntity<PostDTO> getPost(@PathVariable String postId) {
        return ResponseEntity.ok(postService.getPost(postId));
    }

    @PutMapping("/api/posts/{postId}")
    public ResponseEntity<PostDTO> updatePost(
            @PathVariable String postId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdatePostRequest request) {
        return ResponseEntity.ok(postService.updatePost(postId, userId, request));
    }

    @DeleteMapping("/api/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable String postId,
            @RequestHeader("X-User-Id") String userId) {
        postService.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/posts/{postId}/like")
    public ResponseEntity<LikeResponse> likePost(
            @PathVariable String postId,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(postService.likePost(postId, userId));
    }

    @DeleteMapping("/api/posts/{postId}/like")
    public ResponseEntity<LikeResponse> unlikePost(
            @PathVariable String postId,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(postService.unlikePost(postId, userId));
    }

    @GetMapping("/api/users/{userId}/posts")
    public ResponseEntity<Page<PostDTO>> getUserPosts(
            @PathVariable String userId,
            Pageable pageable) {
        return ResponseEntity.ok(postService.getUserPosts(userId, pageable));
    }

    @GetMapping("/api/feed")
    public ResponseEntity<Page<PostDTO>> getFeed(
            @RequestHeader("X-User-Id") String userId,
            Pageable pageable) {
        return ResponseEntity.ok(postService.getFeed(userId, pageable));
    }

    @GetMapping("/api/posts/hashtag/{hashtag}")
    public ResponseEntity<Page<PostDTO>> getPostsByHashtag(
            @PathVariable String hashtag,
            Pageable pageable) {
        return ResponseEntity.ok(postService.getPostsByHashtag(hashtag, pageable));
    }

    @GetMapping("/api/posts/trending")
    public ResponseEntity<Page<PostDTO>> getTrendingPosts(Pageable pageable) {
        return ResponseEntity.ok(postService.getTrendingPosts(pageable));
    }

    @PostMapping("/api/posts/{postId}/view")
    public ResponseEntity<Void> recordView(
            @PathVariable String postId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        postService.recordView(postId);
        return ResponseEntity.ok().build();
    }
}

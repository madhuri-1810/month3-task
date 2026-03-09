package com.socialmedia.post.service;

import com.socialmedia.post.dto.*;
import com.socialmedia.post.model.Post;
import com.socialmedia.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;

    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    @Transactional
    public PostDTO createPost(String userId, String username, CreatePostRequest request) {
        Set<String> hashtags = extractHashtags(request.getContent());
        Set<String> mentions = extractMentions(request.getContent());

        Post post = Post.builder()
                .authorId(userId)
                .authorUsername(username)
                .content(request.getContent())
                .mediaUrls(request.getMediaUrls() != null ? request.getMediaUrls() : new ArrayList<>())
                .hashtags(hashtags)
                .mentions(mentions)
                .visibility(request.getVisibility() != null ?
                        Post.Visibility.valueOf(request.getVisibility()) : Post.Visibility.PUBLIC)
                .build();

        post = postRepository.save(post);
        log.info("Post created by user: {}", userId);

        // Publish event to RabbitMQ
        PostCreatedEvent event = PostCreatedEvent.builder()
                .postId(post.getId())
                .authorId(userId)
                .authorUsername(username)
                .content(post.getContent())
                .hashtags(hashtags)
                .mentions(mentions)
                .createdAt(post.getCreatedAt())
                .build();
        rabbitTemplate.convertAndSend("social-media.exchange", "post.created", event);

        return mapToDTO(post);
    }

    public PostDTO getPost(String postId) {
        Post post = findPostById(postId);
        return mapToDTO(post);
    }

    @Transactional
    public PostDTO updatePost(String postId, String userId, UpdatePostRequest request) {
        Post post = findPostById(postId);

        if (!post.getAuthorId().equals(userId)) {
            throw new RuntimeException("Unauthorized to update this post");
        }

        if (request.getContent() != null) {
            post.setContent(request.getContent());
            post.setHashtags(extractHashtags(request.getContent()));
            post.setMentions(extractMentions(request.getContent()));
        }
        if (request.getVisibility() != null) {
            post.setVisibility(Post.Visibility.valueOf(request.getVisibility()));
        }

        post = postRepository.save(post);
        return mapToDTO(post);
    }

    @Transactional
    public void deletePost(String postId, String userId) {
        Post post = findPostById(postId);

        if (!post.getAuthorId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this post");
        }

        post.setActive(false);
        postRepository.save(post);
        log.info("Post {} deleted by user {}", postId, userId);
    }

    @Transactional
    public LikeResponse likePost(String postId, String userId) {
        Post post = findPostById(postId);

        if (post.getLikedByUserIds().contains(userId)) {
            throw new RuntimeException("Already liked this post");
        }

        post.getLikedByUserIds().add(userId);
        postRepository.save(post);

        // Publish like event
        PostLikedEvent event = new PostLikedEvent(postId, userId, post.getAuthorId());
        rabbitTemplate.convertAndSend("social-media.exchange", "post.liked", event);

        return new LikeResponse(true, post.getLikedByUserIds().size());
    }

    @Transactional
    public LikeResponse unlikePost(String postId, String userId) {
        Post post = findPostById(postId);
        post.getLikedByUserIds().remove(userId);
        postRepository.save(post);
        return new LikeResponse(false, post.getLikedByUserIds().size());
    }

    public Page<PostDTO> getUserPosts(String userId, Pageable pageable) {
        return postRepository.findByAuthorIdAndIsActiveTrue(userId, pageable)
                .map(this::mapToDTO);
    }

    public Page<PostDTO> getFeed(String userId, Pageable pageable) {
        // Get list of followed user IDs from User Service
        try {
            String[] followingIds = restTemplate.getForObject(
                    "http://user-service/api/users/internal/followers/" + userId,
                    String[].class
            );
            List<String> authorIds = followingIds != null ? Arrays.asList(followingIds) : new ArrayList<>();
            authorIds.add(userId); // Include own posts

            return postRepository.findFeedByAuthorIds(authorIds, pageable)
                    .map(this::mapToDTO);
        } catch (Exception e) {
            log.warn("Could not fetch following list, returning public feed");
            return postRepository.findPublicPosts(pageable).map(this::mapToDTO);
        }
    }

    public Page<PostDTO> getPostsByHashtag(String hashtag, Pageable pageable) {
        return postRepository.findByHashtagsContaining(hashtag.toLowerCase(), pageable)
                .map(this::mapToDTO);
    }

    public Page<PostDTO> getTrendingPosts(Pageable pageable) {
        return postRepository.findTrendingPosts(pageable).map(this::mapToDTO);
    }

    public void recordView(String postId) {
        postRepository.incrementViewCount(postId);
    }

    private Set<String> extractHashtags(String content) {
        if (content == null) return new HashSet<>();
        Set<String> hashtags = new HashSet<>();
        Matcher matcher = HASHTAG_PATTERN.matcher(content);
        while (matcher.find()) {
            hashtags.add(matcher.group(1).toLowerCase());
        }
        return hashtags;
    }

    private Set<String> extractMentions(String content) {
        if (content == null) return new HashSet<>();
        Set<String> mentions = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }
        return mentions;
    }

    private Post findPostById(String postId) {
        return postRepository.findByIdAndIsActiveTrue(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
    }

    private PostDTO mapToDTO(Post post) {
        return PostDTO.builder()
                .id(post.getId())
                .authorId(post.getAuthorId())
                .authorUsername(post.getAuthorUsername())
                .authorDisplayName(post.getAuthorDisplayName())
                .authorProfileImageUrl(post.getAuthorProfileImageUrl())
                .content(post.getContent())
                .mediaUrls(post.getMediaUrls())
                .hashtags(post.getHashtags())
                .mentions(post.getMentions())
                .likesCount(post.getLikedByUserIds().size())
                .commentCount(post.getCommentCount())
                .shareCount(post.getShareCount())
                .viewCount(post.getViewCount())
                .visibility(post.getVisibility().name())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}

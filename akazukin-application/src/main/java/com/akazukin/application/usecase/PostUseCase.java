package com.akazukin.application.usecase;

import com.akazukin.application.dto.PostRequestDto;
import com.akazukin.application.dto.PostResponseDto;
import com.akazukin.application.dto.PostTargetDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.PostTarget;
import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.PostPublisher;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.PostTargetRepository;
import com.akazukin.domain.port.SnsAccountRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class PostUseCase {

    private static final Logger LOG = Logger.getLogger(PostUseCase.class.getName());

    private final PostRepository postRepository;
    private final PostTargetRepository postTargetRepository;
    private final SnsAccountRepository snsAccountRepository;
    private final PostPublisher postPublisher;

    @Inject
    public PostUseCase(PostRepository postRepository,
                       PostTargetRepository postTargetRepository,
                       SnsAccountRepository snsAccountRepository,
                       PostPublisher postPublisher) {
        this.postRepository = postRepository;
        this.postTargetRepository = postTargetRepository;
        this.snsAccountRepository = snsAccountRepository;
        this.postPublisher = postPublisher;
    }

    public PostResponseDto createPost(UUID userId, PostRequestDto request) {
        if (request.content() == null || request.content().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Post content is required");
        }
        if (request.targetPlatforms() == null || request.targetPlatforms().isEmpty()) {
            throw new DomainException("INVALID_INPUT",
                    "At least one target platform is required");
        }

        Instant now = Instant.now();
        boolean isScheduled = request.scheduledAt() != null;

        if (isScheduled && !request.scheduledAt().isAfter(now)) {
            throw new DomainException("INVALID_SCHEDULE",
                    "Scheduled time must be in the future");
        }

        PostStatus postStatus = isScheduled ? PostStatus.SCHEDULED : PostStatus.PUBLISHING;

        Post post = new Post(
                UUID.randomUUID(),
                userId,
                request.content(),
                request.mediaUrls() != null ? request.mediaUrls() : List.of(),
                postStatus,
                request.scheduledAt(),
                now,
                now
        );

        Post savedPost = postRepository.save(post);

        List<PostTarget> targets = createTargets(
                savedPost, userId, request.targetPlatforms(), postStatus, now);

        if (isScheduled) {
            postPublisher.schedulePost(savedPost.getId(), request.scheduledAt());
            LOG.log(Level.INFO, "Post {0} scheduled for {1}",
                    new Object[]{savedPost.getId(), request.scheduledAt()});
        } else {
            postPublisher.publishForProcessing(savedPost.getId());
            LOG.log(Level.INFO, "Post {0} sent for immediate processing",
                    savedPost.getId());
        }

        return toPostResponseDto(savedPost, targets);
    }

    public PostResponseDto updatePost(UUID postId, UUID userId, PostRequestDto request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (!post.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this post");
        }

        if (!post.isEditable()) {
            throw new DomainException("POST_NOT_EDITABLE",
                    "Post cannot be updated in status: " + post.getStatus());
        }

        if (request.content() == null || request.content().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Post content is required");
        }
        if (request.targetPlatforms() == null || request.targetPlatforms().isEmpty()) {
            throw new DomainException("INVALID_INPUT",
                    "At least one target platform is required");
        }

        Instant now = Instant.now();
        boolean wasScheduled = post.getStatus() == PostStatus.SCHEDULED;
        boolean isScheduled = request.scheduledAt() != null;

        if (isScheduled && !request.scheduledAt().isAfter(now)) {
            throw new DomainException("INVALID_SCHEDULE",
                    "Scheduled time must be in the future");
        }

        if (wasScheduled) {
            postPublisher.cancelScheduledPost(postId);
        }

        PostStatus newStatus = isScheduled ? PostStatus.SCHEDULED : PostStatus.PUBLISHING;

        post.setContent(request.content());
        post.setMediaUrls(request.mediaUrls() != null ? request.mediaUrls() : List.of());
        post.setStatus(newStatus);
        post.setScheduledAt(request.scheduledAt());
        post.setUpdatedAt(now);

        Post savedPost = postRepository.save(post);

        postTargetRepository.deleteByPostId(postId);
        List<PostTarget> targets = createTargets(
                savedPost, userId, request.targetPlatforms(), newStatus, now);

        if (isScheduled) {
            postPublisher.schedulePost(savedPost.getId(), request.scheduledAt());
            LOG.log(Level.INFO, "Post {0} rescheduled for {1}",
                    new Object[]{savedPost.getId(), request.scheduledAt()});
        } else {
            postPublisher.publishForProcessing(savedPost.getId());
            LOG.log(Level.INFO, "Post {0} sent for immediate processing after update",
                    savedPost.getId());
        }

        return toPostResponseDto(savedPost, targets);
    }

    public PostResponseDto getPost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        List<PostTarget> targets = postTargetRepository.findByPostId(postId);

        return toPostResponseDto(post, targets);
    }

    public List<PostResponseDto> listPosts(UUID userId, int page, int size) {
        int offset = page * size;
        List<Post> posts = postRepository.findByUserId(userId, offset, size);

        List<UUID> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        List<PostTarget> allTargets = postTargetRepository.findByPostIds(postIds);
        Map<UUID, List<PostTarget>> targetsByPostId = allTargets.stream()
                .collect(Collectors.groupingBy(PostTarget::getPostId));

        List<PostResponseDto> result = new ArrayList<>();
        for (Post post : posts) {
            List<PostTarget> targets = targetsByPostId.getOrDefault(post.getId(), List.of());
            result.add(toPostResponseDto(post, targets));
        }

        return result;
    }

    public void deletePost(UUID postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (!post.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this post");
        }

        if (!post.isEditable()) {
            throw new DomainException("POST_NOT_EDITABLE",
                    "Post cannot be deleted in status: " + post.getStatus());
        }

        if (post.getStatus() == PostStatus.SCHEDULED) {
            postPublisher.cancelScheduledPost(postId);
        }

        postTargetRepository.deleteByPostId(postId);
        postRepository.deleteById(postId);
    }

    private List<PostTarget> createTargets(Post post, UUID userId,
                                           List<String> targetPlatforms,
                                           PostStatus status, Instant now) {
        List<PostTarget> targets = new ArrayList<>();
        for (String platformStr : targetPlatforms) {
            SnsPlatform platform = SnsPlatform.valueOf(platformStr.toUpperCase());

            SnsAccount account = snsAccountRepository
                    .findByUserIdAndPlatform(userId, platform)
                    .orElseThrow(() -> new DomainException("ACCOUNT_NOT_CONNECTED",
                            "No connected account for platform: " + platform));

            PostTarget target = new PostTarget(
                    UUID.randomUUID(),
                    post.getId(),
                    account.getId(),
                    platform,
                    null,
                    status,
                    null,
                    null,
                    now
            );

            targets.add(postTargetRepository.save(target));
        }
        return targets;
    }

    private PostResponseDto toPostResponseDto(Post post, List<PostTarget> targets) {
        List<PostTargetDto> targetDtos = targets.stream()
                .map(this::toPostTargetDto)
                .toList();

        return new PostResponseDto(
                post.getId(),
                post.getContent(),
                post.getStatus().name(),
                post.getScheduledAt(),
                post.getCreatedAt(),
                targetDtos
        );
    }

    private PostTargetDto toPostTargetDto(PostTarget target) {
        return new PostTargetDto(
                target.getId(),
                target.getPlatform().name(),
                target.getStatus().name(),
                target.getPlatformPostId(),
                target.getErrorMessage(),
                target.getPublishedAt()
        );
    }
}

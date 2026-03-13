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
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.PostTargetRepository;
import com.akazukin.domain.port.SnsAccountRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PostUseCase {

    private final PostRepository postRepository;
    private final PostTargetRepository postTargetRepository;
    private final SnsAccountRepository snsAccountRepository;

    @Inject
    public PostUseCase(PostRepository postRepository,
                       PostTargetRepository postTargetRepository,
                       SnsAccountRepository snsAccountRepository) {
        this.postRepository = postRepository;
        this.postTargetRepository = postTargetRepository;
        this.snsAccountRepository = snsAccountRepository;
    }

    public PostResponseDto createPost(UUID userId, PostRequestDto request) {
        if (request.content() == null || request.content().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Post content is required");
        }
        if (request.targetPlatforms() == null || request.targetPlatforms().isEmpty()) {
            throw new DomainException("INVALID_INPUT", "At least one target platform is required");
        }

        Instant now = Instant.now();
        boolean isScheduled = request.scheduledAt() != null;
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

        List<PostTarget> targets = new ArrayList<>();
        for (String platformStr : request.targetPlatforms()) {
            SnsPlatform platform = SnsPlatform.valueOf(platformStr.toUpperCase());

            SnsAccount account = snsAccountRepository.findByUserIdAndPlatform(userId, platform)
                    .orElseThrow(() -> new DomainException("ACCOUNT_NOT_CONNECTED",
                            "No connected account for platform: " + platform));

            PostTarget target = new PostTarget(
                    UUID.randomUUID(),
                    savedPost.getId(),
                    account.getId(),
                    platform,
                    null,
                    postStatus,
                    null,
                    null,
                    now
            );

            targets.add(postTargetRepository.save(target));
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

        List<PostResponseDto> result = new ArrayList<>();
        for (Post post : posts) {
            List<PostTarget> targets = postTargetRepository.findByPostId(post.getId());
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

        postTargetRepository.deleteByPostId(postId);
        postRepository.deleteById(postId);
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

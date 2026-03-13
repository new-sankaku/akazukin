package com.akazukin.application.usecase;

import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.PostTarget;
import com.akazukin.domain.port.PostPublisher;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.PostTargetRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class SchedulePostUseCase {

    private static final Logger LOG = Logger.getLogger(SchedulePostUseCase.class.getName());

    private final PostRepository postRepository;
    private final PostTargetRepository postTargetRepository;
    private final PostPublisher postPublisher;
    private final PostPublishUseCase postPublishUseCase;

    @Inject
    public SchedulePostUseCase(PostRepository postRepository,
                               PostTargetRepository postTargetRepository,
                               PostPublisher postPublisher,
                               PostPublishUseCase postPublishUseCase) {
        this.postRepository = postRepository;
        this.postTargetRepository = postTargetRepository;
        this.postPublisher = postPublisher;
        this.postPublishUseCase = postPublishUseCase;
    }

    public void processScheduledPost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        LOG.log(Level.INFO, "Processing scheduled post {0} (current status: {1})",
                new Object[]{postId, post.getStatus()});

        postPublishUseCase.processPost(postId);
    }

    public List<Post> findPostsDueForPublishing() {
        return postRepository.findScheduledBefore(Instant.now());
    }

    public void cancelScheduledPost(UUID postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (!post.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this post");
        }

        if (post.getStatus() != PostStatus.SCHEDULED) {
            throw new DomainException("POST_NOT_SCHEDULED",
                    "Post cannot be cancelled in status: " + post.getStatus());
        }

        LOG.log(Level.INFO, "Cancelling scheduled post {0}", postId);

        postPublisher.cancelScheduledPost(postId);

        post.setStatus(PostStatus.DRAFT);
        post.setScheduledAt(null);
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);

        List<PostTarget> targets = postTargetRepository.findByPostId(postId);
        for (PostTarget target : targets) {
            target.setStatus(PostStatus.DRAFT);
            postTargetRepository.save(target);
        }

        LOG.log(Level.INFO, "Successfully cancelled scheduled post {0}", postId);
    }

    public void reschedulePost(UUID postId, UUID userId, Instant newScheduledAt) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (!post.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this post");
        }

        if (post.getStatus() != PostStatus.SCHEDULED) {
            throw new DomainException("POST_NOT_SCHEDULED",
                    "Post cannot be rescheduled in status: " + post.getStatus());
        }

        if (newScheduledAt == null || !newScheduledAt.isAfter(Instant.now())) {
            throw new DomainException("INVALID_SCHEDULE",
                    "Scheduled time must be in the future");
        }

        LOG.log(Level.INFO, "Rescheduling post {0} from {1} to {2}",
                new Object[]{postId, post.getScheduledAt(), newScheduledAt});

        postPublisher.cancelScheduledPost(postId);

        post.setScheduledAt(newScheduledAt);
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);

        postPublisher.schedulePost(postId, newScheduledAt);

        LOG.log(Level.INFO, "Successfully rescheduled post {0} to {1}",
                new Object[]{postId, newScheduledAt});
    }
}

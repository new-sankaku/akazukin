package com.akazukin.application.usecase;

import com.akazukin.domain.exception.AccountNotFoundException;
import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.PostTarget;
import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.PostTargetRepository;
import com.akazukin.domain.port.SnsAccountRepository;
import com.akazukin.domain.port.SnsAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class PostPublishUseCase {

    private static final Logger LOG = Logger.getLogger(PostPublishUseCase.class.getName());

    private final PostRepository postRepository;
    private final PostTargetRepository postTargetRepository;
    private final SnsAccountRepository snsAccountRepository;
    private final SnsAdapterLookup snsAdapterLookup;

    @Inject
    public PostPublishUseCase(PostRepository postRepository,
                              PostTargetRepository postTargetRepository,
                              SnsAccountRepository snsAccountRepository,
                              SnsAdapterLookup snsAdapterLookup) {
        this.postRepository = postRepository;
        this.postTargetRepository = postTargetRepository;
        this.snsAccountRepository = snsAccountRepository;
        this.snsAdapterLookup = snsAdapterLookup;
    }

    public void processPost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (post.getStatus() != PostStatus.PUBLISHING
                && post.getStatus() != PostStatus.SCHEDULED) {
            LOG.log(Level.WARNING, "Post {0} is in unexpected status {1}, skipping processing",
                    new Object[]{postId, post.getStatus()});
            return;
        }

        post.setStatus(PostStatus.PUBLISHING);
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);

        List<PostTarget> targets = postTargetRepository.findByPostId(postId);
        if (targets.isEmpty()) {
            LOG.log(Level.WARNING, "Post {0} has no targets, marking as FAILED", postId);
            post.setStatus(PostStatus.FAILED);
            post.setUpdatedAt(Instant.now());
            postRepository.save(post);
            return;
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = targets.stream()
                .map(target -> CompletableFuture.runAsync(() -> {
                    try {
                        publishTarget(post, target);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        LOG.log(Level.SEVERE, String.format(
                                "Failed to publish post %s to target %s (%s)",
                                postId, target.getId(), target.getPlatform()), e);
                        markTargetFailed(target, e.getMessage());
                    }
                }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        updatePostStatus(post, successCount.get(), failureCount.get(), targets.size());
    }

    private void publishTarget(Post post, PostTarget target) {
        target.setStatus(PostStatus.PUBLISHING);
        postTargetRepository.save(target);

        SnsAccount account = snsAccountRepository.findById(target.getSnsAccountId())
                .orElseThrow(() -> new AccountNotFoundException(target.getSnsAccountId()));

        SnsAdapter adapter = snsAdapterLookup.getAdapter(target.getPlatform());

        PostRequest postRequest = new PostRequest(post.getContent(), post.getMediaUrls());

        LOG.log(Level.INFO, "Publishing post {0} to {1} via account {2}",
                new Object[]{post.getId(), target.getPlatform(), account.getId()});

        PostResult result = adapter.post(account.getAccessToken(), postRequest);

        target.setStatus(PostStatus.PUBLISHED);
        target.setPlatformPostId(result.platformPostId());
        target.setPublishedAt(result.publishedAt() != null ? result.publishedAt() : Instant.now());
        target.setErrorMessage(null);
        postTargetRepository.save(target);

        LOG.log(Level.INFO, "Successfully published post {0} to {1}, platform post ID: {2}",
                new Object[]{post.getId(), target.getPlatform(), result.platformPostId()});
    }

    private void markTargetFailed(PostTarget target, String errorMessage) {
        postTargetRepository.updateStatus(target.getId(), PostStatus.FAILED, errorMessage);
    }

    private void updatePostStatus(Post post, int successCount, int failureCount, int totalCount) {
        PostStatus finalStatus;
        if (successCount == totalCount) {
            finalStatus = PostStatus.PUBLISHED;
        } else if (failureCount == totalCount) {
            finalStatus = PostStatus.FAILED;
        } else {
            finalStatus = PostStatus.PUBLISHED;
            LOG.log(Level.WARNING, "Post {0} partially published: {1}/{2} succeeded",
                    new Object[]{post.getId(), successCount, totalCount});
        }

        post.setStatus(finalStatus);
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);

        LOG.log(Level.INFO, "Post {0} final status: {1} (success={2}, failure={3}, total={4})",
                new Object[]{post.getId(), finalStatus, successCount, failureCount, totalCount});
    }
}

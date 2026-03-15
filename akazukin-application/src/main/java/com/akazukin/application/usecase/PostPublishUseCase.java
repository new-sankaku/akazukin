package com.akazukin.application.usecase;

import com.akazukin.domain.exception.AccountNotFoundException;
import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.PostTarget;
import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.port.CircuitBreakerRegistry;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.PostTargetRepository;
import com.akazukin.domain.port.SnsAccountRepository;
import com.akazukin.domain.port.SnsAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class PostPublishUseCase {

    private static final Logger LOG = Logger.getLogger(PostPublishUseCase.class.getName());

    private static final int MAX_RETRY_ATTEMPTS = 2;
    private static final long INITIAL_BACKOFF_MS = 1000;

    private static final ExecutorService PUBLISH_EXECUTOR =
            Executors.newFixedThreadPool(
                    Math.min(Runtime.getRuntime().availableProcessors() * 2, 16));

    private final PostRepository postRepository;
    private final PostTargetRepository postTargetRepository;
    private final SnsAccountRepository snsAccountRepository;
    private final SnsAdapterLookup snsAdapterLookup;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Inject
    public PostPublishUseCase(PostRepository postRepository,
                              PostTargetRepository postTargetRepository,
                              SnsAccountRepository snsAccountRepository,
                              SnsAdapterLookup snsAdapterLookup,
                              CircuitBreakerRegistry circuitBreakerRegistry) {
        this.postRepository = postRepository;
        this.postTargetRepository = postTargetRepository;
        this.snsAccountRepository = snsAccountRepository;
        this.snsAdapterLookup = snsAdapterLookup;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    public void processPost(UUID postId) {
        long perfStart = System.nanoTime();
        try {
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
                            publishTargetWithRetry(post, target);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                            LOG.log(Level.SEVERE, String.format(
                                    "Failed to publish post %s to target %s (%s) after all retry attempts",
                                    postId, target.getId(), target.getPlatform()), e);
                            markTargetFailed(target, e.getMessage());
                        }
                    }, PUBLISH_EXECUTOR))
                    .toList();

            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(60, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LOG.log(Level.WARNING,
                        "Publishing timed out after 60 seconds for post " + post.getId());
            } catch (ExecutionException e) {
                LOG.log(Level.SEVERE,
                        "Publishing failed for post " + post.getId(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.log(Level.WARNING,
                        "Publishing interrupted for post " + post.getId());
            }

            updatePostStatus(post, successCount.get(), failureCount.get(), targets.size());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostPublishUseCase.processPost", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"PostPublishUseCase.processPost", perfMs});
            }
        }
    }

    private void publishTargetWithRetry(Post post, PostTarget target) {
        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                publishTarget(post, target);
                return;
            } catch (Exception e) {
                lastException = e;
                if (!isRetryable(e) || attempt == MAX_RETRY_ATTEMPTS) {
                    throw e instanceof RuntimeException re ? re : new RuntimeException(e);
                }
                long backoffMs = INITIAL_BACKOFF_MS * (1L << attempt);
                LOG.log(Level.WARNING,
                        "Retryable error publishing post {0} to {1} (attempt {2}/{3}). "
                                + "Retrying in {4}ms: {5}",
                        new Object[]{post.getId(), target.getPlatform(),
                                attempt + 1, MAX_RETRY_ATTEMPTS + 1,
                                backoffMs, e.getMessage()});
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e instanceof RuntimeException re ? re : new RuntimeException(e);
                }
            }
        }
        // This line should be unreachable, but satisfies the compiler
        throw lastException instanceof RuntimeException re ? re : new RuntimeException(lastException);
    }

    private boolean isRetryable(Exception e) {
        // IOException and its subclasses (HttpTimeoutException, ConnectException, etc.)
        if (e instanceof IOException) {
            return true;
        }
        // Unwrap cause chain for IOException wrapped in RuntimeException
        Throwable cause = e.getCause();
        if (cause instanceof IOException) {
            return true;
        }
        // RetryableRateLimitException from adapter layer (checked by class name
        // to avoid application layer depending on adapter layer)
        String className = e.getClass().getSimpleName();
        if (className.startsWith("Retryable")) {
            return true;
        }
        return false;
    }

    private void publishTarget(Post post, PostTarget target) {
        target.setStatus(PostStatus.PUBLISHING);
        postTargetRepository.save(target);

        // Check circuit breaker state
        if (circuitBreakerRegistry != null && !circuitBreakerRegistry.isCallPermitted(target.getPlatform())) {
            throw new RuntimeException("Circuit breaker is open for " + target.getPlatform()
                + ". Publishing is temporarily suspended due to repeated failures.");
        }

        SnsAccount account = snsAccountRepository.findById(target.getSnsAccountId())
                .orElseThrow(() -> new AccountNotFoundException(target.getSnsAccountId()));

        SnsAdapter adapter = snsAdapterLookup.getAdapter(target.getPlatform());

        PostRequest postRequest = new PostRequest(post.getContent(), post.getMediaUrls());

        LOG.log(Level.INFO, "Publishing post {0} to {1} via account {2}",
                new Object[]{post.getId(), target.getPlatform(), account.getId()});

        try {
            PostResult result = adapter.post(account.getAccessToken(), postRequest);

            if (circuitBreakerRegistry != null) {
                circuitBreakerRegistry.recordSuccess(target.getPlatform());
            }

            target.setStatus(PostStatus.PUBLISHED);
            target.setPlatformPostId(result.platformPostId());
            target.setPublishedAt(result.publishedAt() != null ? result.publishedAt() : Instant.now());
            target.setErrorMessage(null);
            postTargetRepository.save(target);

            LOG.log(Level.INFO, "Successfully published post {0} to {1}, platform post ID: {2}",
                    new Object[]{post.getId(), target.getPlatform(), result.platformPostId()});
        } catch (Exception e) {
            if (circuitBreakerRegistry != null) {
                circuitBreakerRegistry.recordFailure(target.getPlatform());
            }
            throw e;
        }
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

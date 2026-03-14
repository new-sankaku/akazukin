package com.akazukin.worker;

import com.akazukin.application.usecase.PostPublishUseCase;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.port.PostRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ScheduledPostPublisher {

    private static final Logger LOG = Logger.getLogger(ScheduledPostPublisher.class.getName());

    private final PostRepository postRepository;
    private final PostPublishUseCase postPublishUseCase;

    @Inject
    public ScheduledPostPublisher(PostRepository postRepository,
                                  PostPublishUseCase postPublishUseCase) {
        this.postRepository = postRepository;
        this.postPublishUseCase = postPublishUseCase;
    }

    @Scheduled(every = "1m", identity = "scheduled-post-publisher")
    void publishScheduledPosts() {
        List<Post> scheduledPosts = postRepository.findScheduledBefore(Instant.now());
        if (scheduledPosts.isEmpty()) {
            return;
        }

        LOG.log(Level.INFO, "Found {0} scheduled posts ready to publish",
                scheduledPosts.size());

        for (Post post : scheduledPosts) {
            try {
                LOG.log(Level.INFO, "Publishing scheduled post {0}", post.getId());
                postPublishUseCase.processPost(post.getId());
            } catch (Exception e) {
                LOG.log(Level.SEVERE,
                        "Failed to publish scheduled post " + post.getId(), e);
            }
        }
    }
}

package com.akazukin.application.usecase;

import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.PostTarget;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.PostTargetRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class SchedulePostUseCase {

    private final PostRepository postRepository;
    private final PostTargetRepository postTargetRepository;

    @Inject
    public SchedulePostUseCase(PostRepository postRepository,
                               PostTargetRepository postTargetRepository) {
        this.postRepository = postRepository;
        this.postTargetRepository = postTargetRepository;
    }

    public void processScheduledPost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        post.setStatus(PostStatus.PUBLISHING);
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);

        List<PostTarget> targets = postTargetRepository.findByPostId(postId);
        for (PostTarget target : targets) {
            target.setStatus(PostStatus.PUBLISHING);
            postTargetRepository.save(target);
        }
    }

    public List<Post> findPostsDueForPublishing() {
        return postRepository.findScheduledBefore(Instant.now());
    }
}

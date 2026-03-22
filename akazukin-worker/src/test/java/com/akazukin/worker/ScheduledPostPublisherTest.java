package com.akazukin.worker;

import com.akazukin.application.usecase.PostPublishUseCase;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.port.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledPostPublisherTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostPublishUseCase postPublishUseCase;

    private ScheduledPostPublisher scheduledPostPublisher;

    @BeforeEach
    void setUp() {
        scheduledPostPublisher = new ScheduledPostPublisher(postRepository, postPublishUseCase);
    }

    @Test
    void publishScheduledPosts_publishesEachScheduledPost() {
        UUID postId1 = UUID.randomUUID();
        UUID postId2 = UUID.randomUUID();
        Post post1 = createPost(postId1);
        Post post2 = createPost(postId2);
        when(postRepository.findScheduledBefore(any(Instant.class)))
                .thenReturn(List.of(post1, post2));

        scheduledPostPublisher.publishScheduledPosts();

        verify(postPublishUseCase).processPost(postId1);
        verify(postPublishUseCase).processPost(postId2);
    }

    @Test
    void publishScheduledPosts_doesNothingWhenNoScheduledPosts() {
        when(postRepository.findScheduledBefore(any(Instant.class)))
                .thenReturn(List.of());

        scheduledPostPublisher.publishScheduledPosts();

        verify(postPublishUseCase, never()).processPost(any());
    }

    @Test
    void publishScheduledPosts_continuesPublishingWhenOnePostFails() {
        UUID postId1 = UUID.randomUUID();
        UUID postId2 = UUID.randomUUID();
        UUID postId3 = UUID.randomUUID();
        Post post1 = createPost(postId1);
        Post post2 = createPost(postId2);
        Post post3 = createPost(postId3);
        when(postRepository.findScheduledBefore(any(Instant.class)))
                .thenReturn(List.of(post1, post2, post3));
        doThrow(new RuntimeException("publish failed"))
                .when(postPublishUseCase).processPost(eq(postId2));

        scheduledPostPublisher.publishScheduledPosts();

        verify(postPublishUseCase).processPost(postId1);
        verify(postPublishUseCase).processPost(postId2);
        verify(postPublishUseCase).processPost(postId3);
    }

    @Test
    void publishScheduledPosts_publishesSinglePost() {
        UUID postId = UUID.randomUUID();
        Post post = createPost(postId);
        when(postRepository.findScheduledBefore(any(Instant.class)))
                .thenReturn(List.of(post));

        scheduledPostPublisher.publishScheduledPosts();

        verify(postPublishUseCase, times(1)).processPost(postId);
    }

    private Post createPost(UUID id) {
        return new Post(id, UUID.randomUUID(), "content", List.of(),
                PostStatus.SCHEDULED, Instant.now().minusSeconds(60),
                Instant.now().minusSeconds(3600), Instant.now().minusSeconds(3600));
    }
}

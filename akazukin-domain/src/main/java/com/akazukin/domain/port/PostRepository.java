package com.akazukin.domain.port;

import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository {

    Optional<Post> findById(UUID id);

    List<Post> findByUserId(UUID userId, int offset, int limit);

    List<Post> findScheduledBefore(Instant before);

    Post save(Post post);

    void deleteById(UUID id);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, PostStatus status);
}

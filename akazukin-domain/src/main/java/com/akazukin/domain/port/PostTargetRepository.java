package com.akazukin.domain.port;

import com.akazukin.domain.model.PostTarget;

import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.SnsPlatform;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PostTargetRepository {

    Optional<PostTarget> findById(UUID id);

    List<PostTarget> findByPostId(UUID postId);

    PostTarget save(PostTarget target);

    void deleteByPostId(UUID postId);

    void updateStatus(UUID id, PostStatus status, String errorMessage);

    List<PostTarget> findByPostIds(List<UUID> postIds);

    Map<String, Long> countByStatusForUser(UUID userId);

    List<PostTarget> findByUserIdAndCreatedAtBetween(UUID userId, Instant from, Instant to);

    List<PostTarget> findByUserIdAndPlatformAndCreatedAtBetween(UUID userId, SnsPlatform platform, Instant from, Instant to);
}

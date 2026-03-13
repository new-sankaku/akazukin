package com.akazukin.domain.port;

import com.akazukin.domain.model.PostTarget;

import com.akazukin.domain.model.PostStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostTargetRepository {

    Optional<PostTarget> findById(UUID id);

    List<PostTarget> findByPostId(UUID postId);

    PostTarget save(PostTarget target);

    void deleteByPostId(UUID postId);

    void updateStatus(UUID id, PostStatus status, String errorMessage);
}

package com.akazukin.domain.port;

import com.akazukin.domain.model.MediaAsset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaAssetRepository {

    Optional<MediaAsset> findById(UUID id);

    List<MediaAsset> findByUserId(UUID userId, int offset, int limit);

    MediaAsset save(MediaAsset mediaAsset);

    void deleteById(UUID id);

    long countByUserId(UUID userId);
}

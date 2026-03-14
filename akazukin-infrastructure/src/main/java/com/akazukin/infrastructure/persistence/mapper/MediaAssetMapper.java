package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.MediaAsset;
import com.akazukin.infrastructure.persistence.entity.MediaAssetEntity;

public final class MediaAssetMapper {

    private MediaAssetMapper() {
    }

    public static MediaAsset toDomain(MediaAssetEntity entity) {
        return new MediaAsset(
                entity.id,
                entity.userId,
                entity.fileName,
                entity.mimeType,
                entity.sizeBytes,
                entity.storageUrl,
                entity.thumbnailUrl,
                entity.altText,
                entity.createdAt
        );
    }

    public static MediaAssetEntity toEntity(MediaAsset domain) {
        MediaAssetEntity entity = new MediaAssetEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.fileName = domain.getFileName();
        entity.mimeType = domain.getMimeType();
        entity.sizeBytes = domain.getSizeBytes();
        entity.storageUrl = domain.getStorageUrl();
        entity.thumbnailUrl = domain.getThumbnailUrl();
        entity.altText = domain.getAltText();
        entity.createdAt = domain.getCreatedAt();
        return entity;
    }
}

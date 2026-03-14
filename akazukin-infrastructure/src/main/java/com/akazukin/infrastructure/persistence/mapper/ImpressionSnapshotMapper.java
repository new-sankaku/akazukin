package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.ImpressionSnapshot;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.infrastructure.persistence.entity.ImpressionSnapshotEntity;

public final class ImpressionSnapshotMapper {

    private ImpressionSnapshotMapper() {
    }

    public static ImpressionSnapshot toDomain(ImpressionSnapshotEntity entity) {
        return new ImpressionSnapshot(
                entity.id,
                entity.snsAccountId,
                SnsPlatform.valueOf(entity.platform),
                entity.followersCount,
                entity.followingCount,
                entity.postCount,
                entity.impressionsCount,
                entity.engagementRate,
                entity.snapshotAt
        );
    }

    public static ImpressionSnapshotEntity toEntity(ImpressionSnapshot domain) {
        ImpressionSnapshotEntity entity = new ImpressionSnapshotEntity();
        entity.id = domain.getId();
        entity.snsAccountId = domain.getSnsAccountId();
        entity.platform = domain.getPlatform().name();
        entity.followersCount = domain.getFollowersCount();
        entity.followingCount = domain.getFollowingCount();
        entity.postCount = domain.getPostCount();
        entity.impressionsCount = domain.getImpressionsCount();
        entity.engagementRate = domain.getEngagementRate();
        entity.snapshotAt = domain.getSnapshotAt();
        return entity;
    }
}

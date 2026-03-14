package com.akazukin.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ImpressionSnapshot {

    private UUID id;
    private UUID snsAccountId;
    private SnsPlatform platform;
    private int followersCount;
    private int followingCount;
    private int postCount;
    private long impressionsCount;
    private double engagementRate;
    private Instant snapshotAt;

    public ImpressionSnapshot(UUID id, UUID snsAccountId, SnsPlatform platform,
                               int followersCount, int followingCount, int postCount,
                               long impressionsCount, double engagementRate, Instant snapshotAt) {
        this.id = id;
        this.snsAccountId = snsAccountId;
        this.platform = platform;
        this.followersCount = followersCount;
        this.followingCount = followingCount;
        this.postCount = postCount;
        this.impressionsCount = impressionsCount;
        this.engagementRate = engagementRate;
        this.snapshotAt = snapshotAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSnsAccountId() {
        return snsAccountId;
    }

    public void setSnsAccountId(UUID snsAccountId) {
        this.snsAccountId = snsAccountId;
    }

    public SnsPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(SnsPlatform platform) {
        this.platform = platform;
    }

    public int getFollowersCount() {
        return followersCount;
    }

    public void setFollowersCount(int followersCount) {
        this.followersCount = followersCount;
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(int followingCount) {
        this.followingCount = followingCount;
    }

    public int getPostCount() {
        return postCount;
    }

    public void setPostCount(int postCount) {
        this.postCount = postCount;
    }

    public long getImpressionsCount() {
        return impressionsCount;
    }

    public void setImpressionsCount(long impressionsCount) {
        this.impressionsCount = impressionsCount;
    }

    public double getEngagementRate() {
        return engagementRate;
    }

    public void setEngagementRate(double engagementRate) {
        this.engagementRate = engagementRate;
    }

    public Instant getSnapshotAt() {
        return snapshotAt;
    }

    public void setSnapshotAt(Instant snapshotAt) {
        this.snapshotAt = snapshotAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImpressionSnapshot that = (ImpressionSnapshot) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

package com.akazukin.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "impression_snapshots")
public class ImpressionSnapshotEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "sns_account_id", nullable = false)
    public UUID snsAccountId;

    @Column(nullable = false, length = 20)
    public String platform;

    @Column(name = "followers_count", nullable = false)
    public int followersCount;

    @Column(name = "following_count", nullable = false)
    public int followingCount;

    @Column(name = "post_count", nullable = false)
    public int postCount;

    @Column(name = "impressions_count", nullable = false)
    public long impressionsCount;

    @Column(name = "engagement_rate", nullable = false)
    public double engagementRate;

    @Column(name = "snapshot_at", nullable = false)
    public Instant snapshotAt;

    @PrePersist
    void prePersist() {
        if (snapshotAt == null) {
            snapshotAt = Instant.now();
        }
    }
}

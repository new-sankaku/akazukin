package com.akazukin.infrastructure.news;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "news_items")
public class NewsItemEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "source_id", nullable = false)
    public UUID sourceId;

    @Column(nullable = false, length = 500)
    public String title;

    @Column(length = 1000)
    public String url;

    @Column(columnDefinition = "TEXT")
    public String summary;

    @Column(name = "published_at")
    public Instant publishedAt;

    @Column(name = "fetched_at", nullable = false)
    public Instant fetchedAt;

    @PrePersist
    void prePersist() {
        if (fetchedAt == null) {
            fetchedAt = Instant.now();
        }
    }
}

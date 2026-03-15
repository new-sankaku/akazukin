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
@Table(name = "media_assets")
public class MediaAssetEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @Column(name = "file_name", nullable = false, length = 255)
    public String fileName;

    @Column(name = "mime_type", nullable = false, length = 100)
    public String mimeType;

    @Column(name = "size_bytes", nullable = false)
    public long sizeBytes;

    @Column(name = "storage_url", nullable = false, length = 1000)
    public String storageUrl;

    @Column(name = "thumbnail_url", length = 1000)
    public String thumbnailUrl;

    @Column(name = "alt_text", length = 500)
    public String altText;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

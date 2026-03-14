package com.akazukin.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "user_id")
    public UUID userId;

    @Column(length = 50)
    public String username;

    @Column(name = "http_method", nullable = false, length = 10)
    public String httpMethod;

    @Column(name = "request_path", nullable = false, length = 2048)
    public String requestPath;

    @Column(name = "query_string", length = 2048)
    public String queryString;

    @Column(name = "request_body", columnDefinition = "TEXT")
    public String requestBody;

    @Column(name = "response_status", nullable = false)
    public int responseStatus;

    @Column(name = "duration_ms", nullable = false)
    public long durationMs;

    @Column(name = "client_ip", length = 45)
    public String clientIp;

    @Column(name = "user_agent", length = 512)
    public String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "category", nullable = false, length = 20)
    public String category = "PAGE";

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

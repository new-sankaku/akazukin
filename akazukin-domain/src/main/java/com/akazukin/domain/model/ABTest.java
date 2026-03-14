package com.akazukin.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ABTest {

    private UUID id;
    private UUID userId;
    private String name;
    private String variantA;
    private String variantB;
    private ABTestStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private String winnerVariant;
    private Instant createdAt;

    public ABTest(UUID id, UUID userId, String name, String variantA, String variantB,
                  ABTestStatus status, Instant startedAt, Instant completedAt,
                  String winnerVariant, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.variantA = variantA;
        this.variantB = variantB;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.winnerVariant = winnerVariant;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVariantA() {
        return variantA;
    }

    public void setVariantA(String variantA) {
        this.variantA = variantA;
    }

    public String getVariantB() {
        return variantB;
    }

    public void setVariantB(String variantB) {
        this.variantB = variantB;
    }

    public ABTestStatus getStatus() {
        return status;
    }

    public void setStatus(ABTestStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getWinnerVariant() {
        return winnerVariant;
    }

    public void setWinnerVariant(String winnerVariant) {
        this.winnerVariant = winnerVariant;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ABTest abTest = (ABTest) o;
        return Objects.equals(id, abTest.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

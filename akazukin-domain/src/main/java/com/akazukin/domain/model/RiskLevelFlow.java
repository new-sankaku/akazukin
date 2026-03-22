package com.akazukin.domain.model;

import java.util.Objects;
import java.util.UUID;

public class RiskLevelFlow {

    private UUID id;
    private UUID teamId;
    private RiskLevel riskLevel;
    private int requiredApprovers;
    private boolean adminRequired;
    private boolean legalReviewRequired;

    public RiskLevelFlow(UUID id, UUID teamId, RiskLevel riskLevel,
                         int requiredApprovers, boolean adminRequired, boolean legalReviewRequired) {
        this.id = id;
        this.teamId = teamId;
        this.riskLevel = riskLevel;
        this.requiredApprovers = requiredApprovers;
        this.adminRequired = adminRequired;
        this.legalReviewRequired = legalReviewRequired;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public int getRequiredApprovers() { return requiredApprovers; }
    public void setRequiredApprovers(int v) { this.requiredApprovers = v; }
    public boolean isAdminRequired() { return adminRequired; }
    public void setAdminRequired(boolean v) { this.adminRequired = v; }
    public boolean isLegalReviewRequired() { return legalReviewRequired; }
    public void setLegalReviewRequired(boolean v) { this.legalReviewRequired = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RiskLevelFlow that = (RiskLevelFlow) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

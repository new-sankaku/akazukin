package com.akazukin.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "risk_level_flows")
public class RiskLevelFlowEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "team_id", nullable = false)
    public UUID teamId;

    @Column(name = "risk_level", length = 20, nullable = false)
    public String riskLevel;

    @Column(name = "required_approvers", nullable = false)
    public int requiredApprovers;

    @Column(name = "admin_required", nullable = false)
    public boolean adminRequired;

    @Column(name = "legal_review_required", nullable = false)
    public boolean legalReviewRequired;
}

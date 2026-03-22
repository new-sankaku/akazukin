package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.RiskLevel;
import com.akazukin.domain.model.RiskLevelFlow;
import com.akazukin.infrastructure.persistence.entity.RiskLevelFlowEntity;

public final class RiskLevelFlowMapper {

    private RiskLevelFlowMapper() {
    }

    public static RiskLevelFlow toDomain(RiskLevelFlowEntity entity) {
        return new RiskLevelFlow(
                entity.id,
                entity.teamId,
                RiskLevel.valueOf(entity.riskLevel),
                entity.requiredApprovers,
                entity.adminRequired,
                entity.legalReviewRequired
        );
    }

    public static RiskLevelFlowEntity toEntity(RiskLevelFlow domain) {
        RiskLevelFlowEntity entity = new RiskLevelFlowEntity();
        entity.id = domain.getId();
        entity.teamId = domain.getTeamId();
        entity.riskLevel = domain.getRiskLevel().name();
        entity.requiredApprovers = domain.getRequiredApprovers();
        entity.adminRequired = domain.isAdminRequired();
        entity.legalReviewRequired = domain.isLegalReviewRequired();
        return entity;
    }
}

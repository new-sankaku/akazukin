package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.ApprovalRule;
import com.akazukin.domain.model.Role;
import com.akazukin.infrastructure.persistence.entity.ApprovalRuleEntity;

public final class ApprovalRuleMapper {

    private ApprovalRuleMapper() {
    }

    public static ApprovalRule toDomain(ApprovalRuleEntity entity) {
        return new ApprovalRule(
                entity.id,
                entity.teamId,
                Role.valueOf(entity.role),
                entity.postApprovalRequired,
                entity.scheduleApprovalRequired,
                entity.mediaApprovalRequired,
                entity.aiCheckRequired,
                entity.aiAutoReject,
                entity.minApprovers,
                entity.approvalDeadlineHours,
                entity.createdAt,
                entity.updatedAt
        );
    }

    public static ApprovalRuleEntity toEntity(ApprovalRule domain) {
        ApprovalRuleEntity entity = new ApprovalRuleEntity();
        entity.id = domain.getId();
        entity.teamId = domain.getTeamId();
        entity.role = domain.getRole().name();
        entity.postApprovalRequired = domain.isPostApprovalRequired();
        entity.scheduleApprovalRequired = domain.isScheduleApprovalRequired();
        entity.mediaApprovalRequired = domain.isMediaApprovalRequired();
        entity.aiCheckRequired = domain.isAiCheckRequired();
        entity.aiAutoReject = domain.isAiAutoReject();
        entity.minApprovers = domain.getMinApprovers();
        entity.approvalDeadlineHours = domain.getApprovalDeadlineHours();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();
        return entity;
    }
}

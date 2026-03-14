package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.ApprovalAction;
import com.akazukin.domain.model.ApprovalRequest;
import com.akazukin.infrastructure.persistence.entity.ApprovalRequestEntity;

public final class ApprovalRequestMapper {

    private ApprovalRequestMapper() {
    }

    public static ApprovalRequest toDomain(ApprovalRequestEntity entity) {
        ApprovalAction action = entity.status != null
                ? ApprovalAction.valueOf(entity.status)
                : null;
        return new ApprovalRequest(
                entity.id,
                entity.postId,
                entity.requesterId,
                entity.approverId,
                entity.teamId,
                action,
                entity.comment,
                entity.requestedAt,
                entity.decidedAt
        );
    }

    public static ApprovalRequestEntity toEntity(ApprovalRequest domain) {
        ApprovalRequestEntity entity = new ApprovalRequestEntity();
        entity.id = domain.getId();
        entity.postId = domain.getPostId();
        entity.requesterId = domain.getRequesterId();
        entity.approverId = domain.getApproverId();
        entity.teamId = domain.getTeamId();
        entity.status = domain.getStatus() != null ? domain.getStatus().name() : null;
        entity.comment = domain.getComment();
        entity.requestedAt = domain.getRequestedAt();
        entity.decidedAt = domain.getDecidedAt();
        return entity;
    }
}

package com.akazukin.domain.port;

import com.akazukin.domain.model.ApprovalAction;
import com.akazukin.domain.model.ApprovalRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalRequestRepository {

    Optional<ApprovalRequest> findById(UUID id);

    Optional<ApprovalRequest> findByPostId(UUID postId);

    List<ApprovalRequest> findPendingByApproverId(UUID approverId, int offset, int limit);

    List<ApprovalRequest> findPendingByTeamId(UUID teamId, int offset, int limit);

    ApprovalRequest save(ApprovalRequest approvalRequest);

    long countPendingByApproverId(UUID approverId);

    long countByTeamIdAndStatus(UUID teamId, ApprovalAction status);

    long countByTeamIdAndStatusAndDecidedAfter(UUID teamId, ApprovalAction status, Instant after);

    List<ApprovalRequest> findByTeamIdAndStatus(UUID teamId, ApprovalAction status);
}

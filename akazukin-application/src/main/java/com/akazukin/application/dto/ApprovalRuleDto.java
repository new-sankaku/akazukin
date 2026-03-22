package com.akazukin.application.dto;

import java.util.UUID;

public record ApprovalRuleDto(
    UUID id,
    UUID teamId,
    String role,
    boolean postApprovalRequired,
    boolean scheduleApprovalRequired,
    boolean mediaApprovalRequired,
    boolean aiCheckRequired,
    boolean aiAutoReject,
    int minApprovers,
    int approvalDeadlineHours
) {}

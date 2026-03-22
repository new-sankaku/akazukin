package com.akazukin.application.dto;

import java.util.List;

public record ApprovalRuleUpdateDto(
    List<RoleApprovalSettingDto> roleSettings,
    boolean aiCheckRequired,
    boolean aiAutoReject,
    int minApprovers,
    int approvalDeadlineHours,
    List<RiskLevelFlowDto> riskLevelFlows
) {}

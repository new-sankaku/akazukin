package com.akazukin.application.dto;

import java.util.List;

public record ApprovalDashboardDto(
    long pendingCount,
    long approvedTodayCount,
    long rejectedCount,
    double aiFailRate,
    List<RejectionTrendDto> rejectionTrends,
    List<TeamApprovalStatusDto> teamStatuses
) {}

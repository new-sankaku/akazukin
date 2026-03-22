package com.akazukin.application.dto;

public record RoleApprovalSettingDto(
    String role,
    boolean postApprovalRequired,
    boolean scheduleApprovalRequired,
    boolean mediaApprovalRequired
) {}

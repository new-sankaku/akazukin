package com.akazukin.application.dto;

public record PostEngagementDto(
    int likeCount,
    int repostCount,
    int replyCount,
    long impressionCount
) {}

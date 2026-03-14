package com.akazukin.application.dto;

public record FriendTargetRequestDto(
    String platform,
    String targetIdentifier,
    String displayName,
    String notes
) {
}

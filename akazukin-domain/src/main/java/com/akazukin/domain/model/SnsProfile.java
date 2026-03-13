package com.akazukin.domain.model;

public record SnsProfile(
    String accountIdentifier,
    String displayName,
    String avatarUrl,
    int followerCount
) {
}

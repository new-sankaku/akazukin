package com.akazukin.application.dto;

import java.util.UUID;

public record FriendComposeRequestDto(
    UUID friendId,
    String purpose,
    String referenceContent
) {}

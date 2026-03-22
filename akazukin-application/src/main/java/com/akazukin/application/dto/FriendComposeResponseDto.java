package com.akazukin.application.dto;

import java.util.UUID;

public record FriendComposeResponseDto(
    UUID friendId,
    String displayName,
    String generatedText,
    String composerNote
) {}

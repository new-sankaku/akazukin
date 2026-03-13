package com.akazukin.application.dto;

import java.util.UUID;

public record AddMemberRequestDto(
    UUID userId,
    String role
) {}

package com.akazukin.application.dto;

public record LoginResponseDto(
    String accessToken,
    String refreshToken,
    long expiresIn
) {}

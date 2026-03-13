package com.akazukin.application.dto;

import java.time.Instant;

public record ErrorResponseDto(
    String error,
    String message,
    String details,
    Instant timestamp
) {
    public static ErrorResponseDto of(String error, String message, String details) {
        return new ErrorResponseDto(error, message, details, Instant.now());
    }
}

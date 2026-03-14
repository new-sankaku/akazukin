package com.akazukin.application.dto;

public record NewsSourceRequestDto(
    String name,
    String url,
    String sourceType
) {}

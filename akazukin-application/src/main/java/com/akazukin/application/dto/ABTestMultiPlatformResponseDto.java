package com.akazukin.application.dto;

import java.util.List;

public record ABTestMultiPlatformResponseDto(
    List<PlatformVariant> variants
) {
    public record PlatformVariant(
        String platform,
        String variant,
        String preview,
        String optimizationNote
    ) {}
}

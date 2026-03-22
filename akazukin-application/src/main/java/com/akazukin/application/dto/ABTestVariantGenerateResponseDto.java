package com.akazukin.application.dto;

public record ABTestVariantGenerateResponseDto(
    String variantA,
    String variantALabel,
    String variantB,
    String variantBLabel,
    String variantBDiff,
    String variantC,
    String variantCLabel,
    String variantCDiff
) {}

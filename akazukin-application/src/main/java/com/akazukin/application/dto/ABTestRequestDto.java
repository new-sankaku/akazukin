package com.akazukin.application.dto;

public record ABTestRequestDto(
    String name,
    String variantA,
    String variantB
) {}

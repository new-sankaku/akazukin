package com.akazukin.application.dto;

import java.util.List;

public record ABTestRequestDto(
    String name,
    String variantA,
    String variantB,
    String variantC,
    List<String> platforms
) {}

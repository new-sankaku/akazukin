package com.akazukin.application.dto;

import java.util.List;
import java.util.UUID;

public record AiCompareRequestDto(
    String prompt,
    List<UUID> personaIds
) {}

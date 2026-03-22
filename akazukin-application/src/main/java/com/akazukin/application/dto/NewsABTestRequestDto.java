package com.akazukin.application.dto;

import java.util.UUID;

public record NewsABTestRequestDto(
    UUID newsItemId,
    UUID personaIdA,
    UUID personaIdB
) {}

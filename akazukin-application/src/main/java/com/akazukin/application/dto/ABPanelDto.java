package com.akazukin.application.dto;

import java.util.UUID;

public record ABPanelDto(
    String label,
    UUID personaId,
    String personaName,
    String generatedText
) {}

package com.akazukin.application.dto;

import java.util.List;

public record AiOllamaStatusDto(
    boolean connected,
    String endpoint,
    String currentModel,
    List<String> availableModels
) {}

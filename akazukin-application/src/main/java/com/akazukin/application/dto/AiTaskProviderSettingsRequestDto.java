package com.akazukin.application.dto;

import java.util.List;

public record AiTaskProviderSettingsRequestDto(
    List<AiTaskProviderSettingDto> settings
) {}

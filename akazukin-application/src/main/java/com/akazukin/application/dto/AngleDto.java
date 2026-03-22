package com.akazukin.application.dto;

import java.util.List;

public record AngleDto(
    String angleType,
    String body,
    List<PlatformFitDto> platformFits
) {}

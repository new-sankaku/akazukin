package com.akazukin.application.dto;

import java.util.List;
import java.util.UUID;

public record MultiAngleResponseDto(
    UUID newsItemId,
    String newsTitle,
    List<AngleDto> angles
) {}

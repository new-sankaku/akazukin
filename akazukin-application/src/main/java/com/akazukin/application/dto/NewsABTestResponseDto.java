package com.akazukin.application.dto;

import java.util.UUID;

public record NewsABTestResponseDto(
    UUID newsItemId,
    String newsTitle,
    ABPanelDto panelA,
    ABPanelDto panelB
) {}

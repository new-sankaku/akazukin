package com.akazukin.application.dto;

import java.util.List;

public record BubbleMapDto(
    List<BubbleItemDto> bubbles,
    List<SnsSummaryDto> snsSummaries,
    List<SnsAdviceDto> snsAdvices
) {}

package com.akazukin.application.dto;

import java.util.List;

public record AiCompareResultDto(
    List<AiCompareColumnDto> columns
) {}

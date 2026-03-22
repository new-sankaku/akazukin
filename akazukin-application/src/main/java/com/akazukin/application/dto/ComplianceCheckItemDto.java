package com.akazukin.application.dto;

public record ComplianceCheckItemDto(
    String lawName,
    String checkItem,
    String result,
    String detail
) {}

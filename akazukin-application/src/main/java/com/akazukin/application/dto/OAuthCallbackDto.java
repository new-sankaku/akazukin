package com.akazukin.application.dto;

public record OAuthCallbackDto(
    String code,
    String state
) {}

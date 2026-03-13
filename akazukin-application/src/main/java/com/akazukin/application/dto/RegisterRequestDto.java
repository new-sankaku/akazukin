package com.akazukin.application.dto;

public record RegisterRequestDto(
    String username,
    String email,
    String password
) {}

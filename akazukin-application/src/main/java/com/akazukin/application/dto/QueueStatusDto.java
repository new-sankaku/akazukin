package com.akazukin.application.dto;

public record QueueStatusDto(
    long pending,
    long processing,
    long completed,
    long failed
) {}

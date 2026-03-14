package com.akazukin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record MediaAssetDto(
    UUID id,
    String fileName,
    String mimeType,
    long sizeBytes,
    String storageUrl,
    String thumbnailUrl,
    String altText,
    Instant createdAt
) {}

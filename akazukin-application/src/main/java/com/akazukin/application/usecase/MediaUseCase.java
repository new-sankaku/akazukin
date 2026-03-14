package com.akazukin.application.usecase;

import com.akazukin.application.dto.MediaAssetDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.MediaAsset;
import com.akazukin.domain.port.MediaAssetRepository;
import com.akazukin.domain.port.MediaStorage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class MediaUseCase {

    private static final Logger LOG = Logger.getLogger(MediaUseCase.class.getName());

    private final MediaAssetRepository mediaAssetRepository;
    private final MediaStorage mediaStorage;

    @Inject
    public MediaUseCase(MediaAssetRepository mediaAssetRepository,
                        MediaStorage mediaStorage) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.mediaStorage = mediaStorage;
    }

    public MediaAssetDto upload(UUID userId, String fileName, String mimeType, byte[] data) {
        if (fileName == null || fileName.isBlank()) {
            throw new DomainException("INVALID_INPUT", "File name is required");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new DomainException("INVALID_INPUT", "MIME type is required");
        }
        if (data == null || data.length == 0) {
            throw new DomainException("INVALID_INPUT", "File data is required");
        }

        String storageUrl = mediaStorage.upload(fileName, mimeType, data);

        Instant now = Instant.now();
        MediaAsset asset = new MediaAsset(
                UUID.randomUUID(),
                userId,
                fileName,
                mimeType,
                data.length,
                storageUrl,
                null,
                null,
                now
        );

        MediaAsset saved = mediaAssetRepository.save(asset);
        LOG.log(Level.INFO, "Media asset uploaded: {0} for user {1}, size: {2} bytes",
                new Object[]{saved.getId(), userId, data.length});

        return toMediaAssetDto(saved);
    }

    public List<MediaAssetDto> listAssets(UUID userId, int page, int size) {
        int offset = page * size;
        return mediaAssetRepository.findByUserId(userId, offset, size).stream()
                .map(this::toMediaAssetDto)
                .toList();
    }

    public void deleteAsset(UUID assetId, UUID userId) {
        MediaAsset asset = mediaAssetRepository.findById(assetId)
                .orElseThrow(() -> new DomainException("ASSET_NOT_FOUND",
                        "Media asset not found: " + assetId));

        if (!asset.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this media asset");
        }

        mediaStorage.delete(asset.getStorageUrl());
        mediaAssetRepository.deleteById(assetId);
        LOG.log(Level.INFO, "Media asset deleted: {0}", assetId);
    }

    private MediaAssetDto toMediaAssetDto(MediaAsset asset) {
        return new MediaAssetDto(
                asset.getId(),
                asset.getFileName(),
                asset.getMimeType(),
                asset.getSizeBytes(),
                asset.getStorageUrl(),
                asset.getThumbnailUrl(),
                asset.getAltText(),
                asset.getCreatedAt()
        );
    }
}

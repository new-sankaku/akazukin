package com.akazukin.application.usecase;

import com.akazukin.application.dto.MediaAssetDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.MediaAsset;
import com.akazukin.domain.port.MediaAssetRepository;
import com.akazukin.domain.port.MediaStorage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaUseCaseTest {

    private InMemoryMediaAssetRepository mediaAssetRepository;
    private InMemoryMediaStorage mediaStorage;
    private MediaUseCase mediaUseCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        mediaAssetRepository = new InMemoryMediaAssetRepository();
        mediaStorage = new InMemoryMediaStorage();
        mediaUseCase = new MediaUseCase(mediaAssetRepository, mediaStorage);

        userId = UUID.randomUUID();
    }

    @Test
    void upload_createsMediaAssetWithCorrectAttributes() {
        byte[] data = "file-content".getBytes();

        MediaAssetDto result = mediaUseCase.upload(userId, "photo.png", "image/png", data);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("photo.png", result.fileName());
        assertEquals("image/png", result.mimeType());
        assertEquals(data.length, result.sizeBytes());
        assertNotNull(result.storageUrl());
        assertNotNull(result.createdAt());
    }

    @Test
    void upload_storesFileInMediaStorage() {
        byte[] data = "file-content".getBytes();

        mediaUseCase.upload(userId, "photo.png", "image/png", data);

        assertEquals(1, mediaStorage.uploadedFiles.size());
        assertEquals("photo.png", mediaStorage.uploadedFiles.get(0));
    }

    @Test
    void upload_persistsAssetInRepository() {
        byte[] data = "file-content".getBytes();

        MediaAssetDto result = mediaUseCase.upload(userId, "photo.png", "image/png", data);

        Optional<MediaAsset> stored = mediaAssetRepository.findById(result.id());
        assertTrue(stored.isPresent());
        assertEquals("photo.png", stored.get().getFileName());
    }

    @Test
    void upload_throwsWhenFileNameIsNull() {
        byte[] data = "file-content".getBytes();

        DomainException exception = assertThrows(DomainException.class,
                () -> mediaUseCase.upload(userId, null, "image/png", data));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void upload_throwsWhenFileNameIsBlank() {
        byte[] data = "file-content".getBytes();

        DomainException exception = assertThrows(DomainException.class,
                () -> mediaUseCase.upload(userId, "", "image/png", data));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void upload_throwsWhenMimeTypeIsNull() {
        byte[] data = "file-content".getBytes();

        DomainException exception = assertThrows(DomainException.class,
                () -> mediaUseCase.upload(userId, "photo.png", null, data));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void upload_throwsWhenMimeTypeIsBlank() {
        byte[] data = "file-content".getBytes();

        DomainException exception = assertThrows(DomainException.class,
                () -> mediaUseCase.upload(userId, "photo.png", "", data));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void upload_throwsWhenDataIsNull() {
        DomainException exception = assertThrows(DomainException.class,
                () -> mediaUseCase.upload(userId, "photo.png", "image/png", null));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void upload_throwsWhenDataIsEmpty() {
        DomainException exception = assertThrows(DomainException.class,
                () -> mediaUseCase.upload(userId, "photo.png", "image/png", new byte[0]));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void listAssets_returnsPaginatedResults() {
        mediaUseCase.upload(userId, "file1.png", "image/png", "a".getBytes());
        mediaUseCase.upload(userId, "file2.png", "image/png", "b".getBytes());
        mediaUseCase.upload(userId, "file3.png", "image/png", "c".getBytes());

        List<MediaAssetDto> result = mediaUseCase.listAssets(userId, 0, 2);

        assertEquals(2, result.size());
    }

    @Test
    void listAssets_returnsEmptyForUserWithNoAssets() {
        UUID otherUserId = UUID.randomUUID();

        List<MediaAssetDto> result = mediaUseCase.listAssets(otherUserId, 0, 10);

        assertTrue(result.isEmpty());
    }

    @Test
    void listAssets_returnsSecondPage() {
        mediaUseCase.upload(userId, "file1.png", "image/png", "a".getBytes());
        mediaUseCase.upload(userId, "file2.png", "image/png", "b".getBytes());
        mediaUseCase.upload(userId, "file3.png", "image/png", "c".getBytes());

        List<MediaAssetDto> result = mediaUseCase.listAssets(userId, 1, 2);

        assertEquals(1, result.size());
    }

    @Test
    void deleteAsset_removesAssetFromRepositoryAndStorage() {
        MediaAssetDto uploaded = mediaUseCase.upload(userId, "photo.png", "image/png", "data".getBytes());

        mediaUseCase.deleteAsset(uploaded.id(), userId);

        assertTrue(mediaAssetRepository.findById(uploaded.id()).isEmpty());
        assertEquals(1, mediaStorage.deletedUrls.size());
    }

    @Test
    void deleteAsset_throwsWhenAssetNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> mediaUseCase.deleteAsset(nonExistentId, userId));
        assertEquals("ASSET_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void deleteAsset_throwsForbiddenWhenNotOwner() {
        MediaAssetDto uploaded = mediaUseCase.upload(userId, "photo.png", "image/png", "data".getBytes());
        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> mediaUseCase.deleteAsset(uploaded.id(), otherUserId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    private static class InMemoryMediaAssetRepository implements MediaAssetRepository {

        private final Map<UUID, MediaAsset> store = new HashMap<>();

        @Override
        public Optional<MediaAsset> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<MediaAsset> findByUserId(UUID userId, int offset, int limit) {
            List<MediaAsset> userAssets = store.values().stream()
                    .filter(asset -> asset.getUserId().equals(userId))
                    .toList();

            int end = Math.min(offset + limit, userAssets.size());
            if (offset >= userAssets.size()) {
                return List.of();
            }
            return new ArrayList<>(userAssets.subList(offset, end));
        }

        @Override
        public MediaAsset save(MediaAsset mediaAsset) {
            store.put(mediaAsset.getId(), mediaAsset);
            return mediaAsset;
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }

        @Override
        public long countByUserId(UUID userId) {
            return store.values().stream()
                    .filter(asset -> asset.getUserId().equals(userId))
                    .count();
        }
    }

    private static class InMemoryMediaStorage implements MediaStorage {

        final List<String> uploadedFiles = new ArrayList<>();
        final List<String> deletedUrls = new ArrayList<>();
        private final Map<String, byte[]> storage = new HashMap<>();

        @Override
        public String upload(String fileName, String mimeType, byte[] data) {
            String url = "storage://" + fileName;
            storage.put(url, data);
            uploadedFiles.add(fileName);
            return url;
        }

        @Override
        public void delete(String storageUrl) {
            storage.remove(storageUrl);
            deletedUrls.add(storageUrl);
        }

        @Override
        public byte[] download(String storageUrl) {
            return storage.get(storageUrl);
        }
    }
}

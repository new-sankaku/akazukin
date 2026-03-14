package com.akazukin.infrastructure.storage;

import com.akazukin.domain.port.MediaStorage;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class LocalMediaStorage implements MediaStorage {
    private static final Logger LOG = Logger.getLogger(LocalMediaStorage.class.getName());
    private static final Path BASE_DIR = Path.of("./data/media");

    @Override
    public String upload(String fileName, String mimeType, byte[] data) {
        try {
            Files.createDirectories(BASE_DIR);
            String storedName = UUID.randomUUID() + "_" + fileName;
            Path filePath = BASE_DIR.resolve(storedName);
            Files.write(filePath, data);
            LOG.log(Level.FINE, "Media uploaded: {0} ({1} bytes)", new Object[]{storedName, data.length});
            return filePath.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to upload media: " + fileName, e);
        }
    }

    @Override
    public void delete(String storageUrl) {
        try {
            Path filePath = Path.of(storageUrl);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete media: " + storageUrl, e);
        }
    }

    @Override
    public byte[] download(String storageUrl) {
        try {
            return Files.readAllBytes(Path.of(storageUrl));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to download media: " + storageUrl, e);
        }
    }
}

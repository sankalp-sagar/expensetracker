package com.sankalp.expensetracker.expense.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageProvider implements FileStorageProvider {

    private final Path baseDir;

    public LocalFileStorageProvider(@Value("${app.storage.local-dir:/app/uploads}") String baseDir) throws IOException {
        this.baseDir = Paths.get(baseDir);
        Files.createDirectories(this.baseDir);
    }

    @Override
    public String store(MultipartFile file, String subPath) throws IOException {
        Path subDir = baseDir.resolve(subPath);
        Files.createDirectories(subDir);
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String safe = original.replaceAll("[^A-Za-z0-9._-]", "_");
        String key = subPath + "/" + UUID.randomUUID() + "_" + safe;
        Path target = baseDir.resolve(key);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Stored upload at {}", target);
        return key;
    }

    @Override
    public String resolveUrl(String storageKey) {
        return "/api/receipts/file/" + storageKey;
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(baseDir.resolve(storageKey));
    }

    public Path resolveLocal(String storageKey) {
        return baseDir.resolve(storageKey);
    }
}

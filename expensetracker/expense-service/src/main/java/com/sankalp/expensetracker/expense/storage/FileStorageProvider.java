package com.sankalp.expensetracker.expense.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Abstract file storage. Default impl writes to local disk; swap to S3 later by changing app.storage.provider.
 */
public interface FileStorageProvider {
    /** Returns the storage key (path/object key) for the persisted file. */
    String store(MultipartFile file, String subPath) throws IOException;

    /** Returns a URL/path the client can fetch (or signed URL for s3). */
    String resolveUrl(String storageKey);

    /** Removes the underlying object/file. */
    void delete(String storageKey) throws IOException;
}

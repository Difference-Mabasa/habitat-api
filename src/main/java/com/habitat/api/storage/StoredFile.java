package com.habitat.api.storage;

/**
 * Result of a successful {@link StorageService#store} call. The
 * {@code storedPath} is opaque to callers (relative to the storage
 * root, never user-facing); pass it back unchanged to {@code open()}
 * or {@code delete()}.
 */
public record StoredFile(String storedPath, String detectedMimeType, long size) {}

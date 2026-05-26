package com.habitat.api.storage;

import java.io.InputStream;

/**
 * Streaming handle returned by {@link StorageService#open}. The caller
 * owns the {@link InputStream} and must close it (try-with-resources
 * inside a {@code StreamingResponseBody} is the canonical pattern).
 */
public record StoredResource(InputStream content, String mimeType, long size) {}

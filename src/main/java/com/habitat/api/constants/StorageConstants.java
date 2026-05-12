package com.habitat.api.constants;

import java.util.Set;

public final class StorageConstants {

    // Subfolders
    public static final String FOLDER_PROPERTY_PHOTOS = "properties";
    public static final String FOLDER_UNIT_PHOTOS = "units";
    public static final String FOLDER_LEASES = "leases";
    public static final String FOLDER_DOCUMENTS = "documents";
    public static final String FOLDER_AVATARS = "avatars";

    // Allowed types (validated via Tika magic bytes, NOT Content-Type)
    public static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    public static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg", "image/png"
    );

    public static final long MAX_IMAGE_BYTES = 8L * 1024 * 1024;    // 8 MB
    public static final long MAX_DOCUMENT_BYTES = 12L * 1024 * 1024; // 12 MB

    private StorageConstants() {}
}

package com.habitat.api.dto.application;

import com.habitat.api.entity.ApplicationDocument;
import com.habitat.api.enums.DocumentType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Per-row uploaded document on an application.
 *
 * <p>{@code downloadUrl} is a fully-qualified API path the UI can hit
 * (subject to its own auth + ownership check). It is built from the
 * application + document ids rather than the underlying storage path
 * so the storage layout stays opaque and we can swap S3 in without a
 * DTO change.
 */
public record ApplicationDocumentResponse(
        UUID id,
        UUID applicationId,
        DocumentType docType,
        String fileName,
        String mimeType,
        Long sizeBytes,
        String downloadUrl,
        OffsetDateTime uploadedAt,
        boolean verified
) {
    public static ApplicationDocumentResponse from(ApplicationDocument d) {
        UUID appId = d.getApplication().getId();
        return new ApplicationDocumentResponse(
                d.getId(),
                appId,
                d.getDocType(),
                d.getFileName(),
                d.getMimeType(),
                d.getSizeBytes(),
                "/api/v1/files/documents/" + appId + "/" + d.getId(),
                d.getUploadedAt(),
                d.isVerified()
        );
    }
}

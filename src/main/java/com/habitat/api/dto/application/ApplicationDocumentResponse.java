package com.habitat.api.dto.application;

import com.habitat.api.entity.ApplicationDocument;
import com.habitat.api.enums.DocumentType;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Per-row uploaded document on an application. */
public record ApplicationDocumentResponse(
        UUID id,
        UUID applicationId,
        DocumentType docType,
        String fileUrl,
        String fileName,
        OffsetDateTime uploadedAt,
        boolean verified
) {
    public static ApplicationDocumentResponse from(ApplicationDocument d) {
        return new ApplicationDocumentResponse(
                d.getId(),
                d.getApplication().getId(),
                d.getDocType(),
                d.getFileUrl(),
                d.getFileName(),
                d.getUploadedAt(),
                d.isVerified()
        );
    }
}

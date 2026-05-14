package com.habitat.api.dto.application;

import com.habitat.api.enums.DocumentType;

import java.util.List;

/**
 * Payload for {@code GET /applications/{id}/documents} — what the
 * landlord wants, alongside what the tenant has already uploaded. The
 * upload screen renders one row per required type, showing the upload
 * widget vs the file metadata depending on whether a matching entry
 * exists in {@code uploaded}.
 */
public record RequiredDocumentsResponse(
        List<DocumentType> required,
        List<ApplicationDocumentResponse> uploaded
) {}

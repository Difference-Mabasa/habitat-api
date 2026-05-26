package com.habitat.api.dto.property;

import com.habitat.api.enums.DocumentType;

import java.util.List;

/**
 * Returned from {@code PUT /properties/{id}/required-documents}.
 * Echoes the canonical set after the upsert so the UI can reconcile
 * without a re-fetch.
 */
public record SetRequiredDocumentsResponse(List<DocumentType> docTypes) {}

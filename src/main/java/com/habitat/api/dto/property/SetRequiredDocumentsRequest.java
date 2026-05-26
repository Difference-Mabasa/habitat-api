package com.habitat.api.dto.property;

import com.habitat.api.enums.DocumentType;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Body for {@code PUT /properties/{id}/required-documents}. The list
 * is a full replacement — every type the landlord wants required,
 * regardless of what's already there. Duplicates are collapsed; an
 * empty list is allowed (zero required docs).
 */
public record SetRequiredDocumentsRequest(
        @NotNull List<DocumentType> docTypes
) {}

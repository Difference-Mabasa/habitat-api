package com.habitat.api.dto.application;

import com.habitat.api.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /applications/{id}/documents}. Carries the doc
 * type + filename + the relative URL once the file is stored. Real
 * multipart support lands when a StorageService is wired; for now the
 * client passes a stub URL containing the original filename.
 */
public record UploadDocumentRequest(
        @NotNull DocumentType docType,

        @NotBlank
        @Size(max = 255)
        String fileName,

        @NotBlank
        @Size(max = 500)
        String fileUrl
) {}

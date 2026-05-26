package com.habitat.api.entity;

import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;

/**
 * A document uploaded by a tenant against an application. Keyed on
 * (application, docType) so re-uploading overwrites the row rather than
 * creating duplicates.
 *
 * <p>Phase 7 made this real: {@code fileUrl} now holds the opaque
 * storage-relative path produced by
 * {@link com.habitat.api.storage.StorageService#store}. The tenant
 * downloads the bytes via the ownership-checked
 * {@code GET /files/documents/{applicationId}/{docId}} endpoint, never
 * by hitting the storage path directly.
 *
 * <p>{@code mimeType} + {@code sizeBytes} are populated from Tika
 * magic-byte detection (NOT the spoofable {@code Content-Type} header,
 * per {@code development-standards.md} §6).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "application_documents")
@SQLRestriction("deleted_at IS NULL")
public class ApplicationDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 40)
    private DocumentType docType;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "mime_type", length = 127)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "uploaded_at", nullable = false)
    @Builder.Default
    private OffsetDateTime uploadedAt = OffsetDateTime.now();

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private boolean verified = false;
}

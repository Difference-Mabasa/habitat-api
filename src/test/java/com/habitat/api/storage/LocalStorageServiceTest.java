package com.habitat.api.storage;

import com.habitat.api.constants.StorageConstants;
import com.habitat.api.exception.BadRequestException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Filesystem-backed storage covered end-to-end against a temp dir. The
 * tests pin the contract callers depend on (validation order, MIME
 * detection, path-traversal guard, re-upload cleanup).
 */
class LocalStorageServiceTest {

    private static final byte[] PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            // IHDR chunk so Tika recognises a valid 1×1 image
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89
    };

    private static final byte[] PDF_BYTES = "%PDF-1.4\n%âãÏÓ\n".getBytes();

    private LocalStorageService newService(Path root) throws IOException {
        return new LocalStorageService(root.toString());
    }

    @Test
    void store_writes_bytes_and_returns_a_relative_path(@TempDir Path tmp) throws IOException {
        LocalStorageService s = newService(tmp);
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", PNG_BYTES);

        StoredFile stored = s.store(StorageConstants.FOLDER_DOCUMENTS, file,
                StorageConstants.ALLOWED_DOCUMENT_TYPES, StorageConstants.MAX_DOCUMENT_BYTES);

        assertThat(stored.storedPath()).startsWith(StorageConstants.FOLDER_DOCUMENTS + "/");
        assertThat(stored.storedPath()).endsWith("-photo.png");
        assertThat(stored.detectedMimeType()).isEqualTo("image/png");
        assertThat(stored.size()).isEqualTo(PNG_BYTES.length);
        assertThat(Files.exists(tmp.resolve(stored.storedPath()))).isTrue();
    }

    @Test
    void store_uses_tika_detected_type_not_client_header(@TempDir Path tmp) throws IOException {
        LocalStorageService s = newService(tmp);
        // Spoof: client claims PDF, payload is actually a PNG. Tika
        // detects the truth and the upload still passes (PNG is allowed)
        // — but the stored MIME is "image/png", not the spoofed header.
        MockMultipartFile file = new MockMultipartFile(
                "file", "claim.pdf", "application/pdf", PNG_BYTES);

        StoredFile stored = s.store(StorageConstants.FOLDER_DOCUMENTS, file,
                StorageConstants.ALLOWED_DOCUMENT_TYPES, StorageConstants.MAX_DOCUMENT_BYTES);

        assertThat(stored.detectedMimeType()).isEqualTo("image/png");
    }

    @Test
    void store_rejects_an_unallowed_type(@TempDir Path tmp) throws IOException {
        LocalStorageService s = newService(tmp);
        // ZIP header — not in ALLOWED_DOCUMENT_TYPES.
        byte[] zip = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x14, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file", "secrets.pdf", "application/pdf", zip);

        assertThatThrownBy(() -> s.store(
                StorageConstants.FOLDER_DOCUMENTS, file,
                StorageConstants.ALLOWED_DOCUMENT_TYPES, StorageConstants.MAX_DOCUMENT_BYTES))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void store_rejects_oversize_uploads(@TempDir Path tmp) throws IOException {
        LocalStorageService s = newService(tmp);
        MockMultipartFile file = new MockMultipartFile(
                "file", "huge.pdf", "application/pdf", PDF_BYTES);

        assertThatThrownBy(() -> s.store(
                StorageConstants.FOLDER_DOCUMENTS, file,
                StorageConstants.ALLOWED_DOCUMENT_TYPES, 5L /* tiny cap */))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void store_rejects_empty_uploads(@TempDir Path tmp) throws IOException {
        LocalStorageService s = newService(tmp);
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> s.store(
                StorageConstants.FOLDER_DOCUMENTS, file,
                StorageConstants.ALLOWED_DOCUMENT_TYPES, StorageConstants.MAX_DOCUMENT_BYTES))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void open_returns_a_readable_stream(@TempDir Path tmp) throws IOException {
        LocalStorageService s = newService(tmp);
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", PDF_BYTES);
        StoredFile stored = s.store(StorageConstants.FOLDER_DOCUMENTS, file,
                StorageConstants.ALLOWED_DOCUMENT_TYPES, StorageConstants.MAX_DOCUMENT_BYTES);

        StoredResource resource = s.open(stored.storedPath());

        assertThat(resource.size()).isEqualTo(PDF_BYTES.length);
        try (InputStream in = resource.content()) {
            assertThat(in.readAllBytes()).isEqualTo(PDF_BYTES);
        }
    }

    @Test
    void open_throws_when_the_stored_path_is_missing(@TempDir Path tmp) throws IOException {
        LocalStorageService s = newService(tmp);

        assertThatThrownBy(() -> s.open("documents/never-existed.pdf"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_is_idempotent(@TempDir Path tmp) throws IOException {
        LocalStorageService s = newService(tmp);
        // Calling delete on something that never existed must not throw.
        s.delete("documents/never-existed.pdf");
        s.delete(null);
        s.delete("");
        assertThat(s.exists("documents/never-existed.pdf")).isFalse();
    }

    @Test
    void path_traversal_attempts_are_blocked(@TempDir Path tmp) throws IOException {
        LocalStorageService s = newService(tmp);
        // Write a sibling file that we should NEVER be able to read.
        Path sibling = tmp.getParent().resolve("escape-target-" + System.nanoTime() + ".txt");
        Files.writeString(sibling, "top secret");
        try {
            String evil = "../" + sibling.getFileName().toString();
            assertThatThrownBy(() -> s.open(evil))
                    .isInstanceOf(ForbiddenException.class);
            // resolveSafe is package-private — also exercise it directly.
            assertThatThrownBy(() -> s.resolveSafe("../../../etc/passwd"))
                    .isInstanceOf(ForbiddenException.class);
        } finally {
            Files.deleteIfExists(sibling);
        }
    }

    @Test
    void resolveSafe_rejects_a_null_input(@TempDir Path tmp) throws IOException {
        LocalStorageService s = newService(tmp);
        assertThatThrownBy(() -> s.resolveSafe(null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void sanitizeFilename_strips_separators_and_traversal_segments() {
        assertThat(LocalStorageService.sanitizeFilename("a/b/c.pdf")).isEqualTo("a_b_c.pdf");
        assertThat(LocalStorageService.sanitizeFilename("..\\evil.pdf")).isEqualTo("__evil.pdf");
        assertThat(LocalStorageService.sanitizeFilename(null)).isEqualTo("upload");
        assertThat(LocalStorageService.sanitizeFilename("")).isEqualTo("upload");
    }

    @Test
    void exists_returns_false_for_an_unknown_path(@TempDir Path tmp) throws IOException {
        LocalStorageService s = newService(tmp);
        assertThat(s.exists("documents/nope.pdf")).isFalse();
        assertThat(s.exists(null)).isFalse();
        assertThat(s.exists("")).isFalse();
    }

    @Test
    void store_then_delete_removes_the_file(@TempDir Path tmp) throws IOException {
        LocalStorageService s = newService(tmp);
        StoredFile stored = s.store(
                StorageConstants.FOLDER_DOCUMENTS,
                new MockMultipartFile("file", "doc.pdf", "application/pdf", PDF_BYTES),
                StorageConstants.ALLOWED_DOCUMENT_TYPES,
                StorageConstants.MAX_DOCUMENT_BYTES);

        assertThat(s.exists(stored.storedPath())).isTrue();
        s.delete(stored.storedPath());
        assertThat(s.exists(stored.storedPath())).isFalse();
    }

    @Test
    void allowed_image_types_set_blocks_documents_folder(@TempDir Path tmp) throws IOException {
        LocalStorageService s = newService(tmp);
        // A real PDF passed into the IMAGE allow-list should be rejected.
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", PDF_BYTES);

        assertThatThrownBy(() -> s.store(StorageConstants.FOLDER_PROPERTY_PHOTOS, file,
                Set.of("image/jpeg", "image/png"), StorageConstants.MAX_IMAGE_BYTES))
                .isInstanceOf(BadRequestException.class);
    }
}

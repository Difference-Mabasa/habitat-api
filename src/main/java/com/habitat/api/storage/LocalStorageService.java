package com.habitat.api.storage;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.exception.BadRequestException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Filesystem-backed {@link StorageService}. Default storage for dev and
 * local-prod single-host deployments. S3-backed sibling will land when
 * we move to multi-instance.
 *
 * <p>Magic-byte detection via Tika happens before the file ever touches
 * disk — a spoofed {@code .pdf} containing an EXE is rejected on the
 * first read.
 *
 * <p>Every disk path is resolved through {@link #resolveSafe} which
 * normalises {@code ..} segments and refuses anything that escapes the
 * storage root. This is the §6 path-traversal guard.
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
@Slf4j
// final to satisfy SpotBugs' CT_CONSTRUCTOR_THROW — a throwing constructor
// on a non-final class is theoretically exploitable via a finalizer
// attack in the subclass. We don't subclass; the modifier just documents
// it.
public final class LocalStorageService implements StorageService {

    private static final int MAX_FILENAME_CHARS = 100;
    private static final Tika TIKA = new Tika();

    private final Path root;

    public LocalStorageService(@Value("${app.storage.local-path:./uploads}") String rootStr) throws IOException {
        this.root = Paths.get(rootStr).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
        log.info("LocalStorageService initialised at {}", this.root);
    }

    @Override
    public StoredFile store(String folder,
                            MultipartFile file,
                            Set<String> allowedMimeTypes,
                            long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(ErrorMessages.FILE_TYPE_NOT_ALLOWED);
        }
        if (file.getSize() > maxBytes) {
            throw new BadRequestException(ErrorMessages.FILE_TOO_LARGE);
        }

        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ServiceUnavailableException(ErrorMessages.SERVICE_UNAVAILABLE, e);
        }
        String detected = TIKA.detect(bytes);
        if (!allowedMimeTypes.contains(detected)) {
            throw new BadRequestException(ErrorMessages.FILE_TYPE_NOT_ALLOWED);
        }

        String safeName = sanitizeFilename(file.getOriginalFilename());
        String storedRel = folder + "/" + UUID.randomUUID() + "-" + safeName;
        Path target = resolveSafe(storedRel);
        Path parent = target.getParent();
        if (parent == null) {
            // resolveSafe enforces target is inside root, and we always
            // prefix with {folder}/, so this is unreachable in production
            // — guard exists to keep SpotBugs (and any future caller that
            // bypasses resolveSafe) honest.
            throw new ServiceUnavailableException(ErrorMessages.SERVICE_UNAVAILABLE);
        }
        try {
            Files.createDirectories(parent);
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new ServiceUnavailableException(ErrorMessages.SERVICE_UNAVAILABLE, e);
        }
        log.info("stored {} ({} bytes, {})", storedRel, bytes.length, detected);
        return new StoredFile(storedRel, detected, bytes.length);
    }

    @Override
    public StoredFile storeTrustedBytes(String folder, String filename, String mimeType, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new com.habitat.api.exception.BadRequestException(ErrorMessages.FILE_TYPE_NOT_ALLOWED);
        }
        String safeName = sanitizeFilename(filename);
        String storedRel = folder + "/" + UUID.randomUUID() + "-" + safeName;
        Path target = resolveSafe(storedRel);
        Path parent = target.getParent();
        if (parent == null) {
            throw new com.habitat.api.exception.ServiceUnavailableException(ErrorMessages.SERVICE_UNAVAILABLE);
        }
        try {
            Files.createDirectories(parent);
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new com.habitat.api.exception.ServiceUnavailableException(ErrorMessages.SERVICE_UNAVAILABLE, e);
        }
        log.info("stored trusted {} ({} bytes, {})", storedRel, bytes.length, mimeType);
        return new StoredFile(storedRel, mimeType, bytes.length);
    }

    @Override
    public StoredResource open(String storedPath) {
        Path target = resolveSafe(storedPath);
        if (!Files.exists(target)) {
            throw new ResourceNotFoundException(ErrorMessages.INVALID_FILE_PATH);
        }
        try {
            String mime = TIKA.detect(target);
            long size = Files.size(target);
            InputStream content = Files.newInputStream(target);
            return new StoredResource(content, mime, size);
        } catch (IOException e) {
            throw new ServiceUnavailableException(ErrorMessages.SERVICE_UNAVAILABLE, e);
        }
    }

    @Override
    public void delete(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return;
        try {
            Path target = resolveSafe(storedPath);
            Files.deleteIfExists(target);
        } catch (ForbiddenException e) {
            // Refuse to delete anything outside the root, but don't blow up
            // the caller — a stale stub URL from before storage shipped is
            // not a fatal condition.
            log.warn("refused to delete out-of-root path: {}", storedPath);
        } catch (IOException e) {
            log.warn("failed to delete {}: {}", storedPath, e.getMessage());
        }
    }

    @Override
    public boolean exists(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return false;
        try {
            return Files.exists(resolveSafe(storedPath));
        } catch (ForbiddenException e) {
            return false;
        }
    }

    /**
     * Normalise + reject path traversal. Matches the {@code development-
     * standards.md} §6 snippet verbatim. Public so tests can pin the
     * behaviour without going through {@code store()}.
     */
    Path resolveSafe(String storedPath) {
        if (storedPath == null) throw new ForbiddenException(ErrorMessages.INVALID_FILE_PATH);
        Path resolved = root.resolve(storedPath).normalize();
        if (!resolved.startsWith(root)) {
            throw new ForbiddenException(ErrorMessages.INVALID_FILE_PATH);
        }
        return resolved;
    }

    /**
     * Keep the original filename for landlord display, but strip
     * separators / control characters / path-traversal segments. We
     * already use a UUID prefix so collisions don't matter; this is
     * purely about not letting "../" or NUL bytes into a path
     * component.
     */
    static String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) return "upload";
        String safe = original
                .replace("\\", "_")
                .replace("/", "_")
                .replace("\0", "_")
                .replace("..", "_");
        // Trim very long names — keep the tail because the extension lives there.
        if (safe.length() > MAX_FILENAME_CHARS) {
            safe = safe.substring(safe.length() - MAX_FILENAME_CHARS);
        }
        return safe;
    }

    Path rootPath() {
        return root;
    }
}

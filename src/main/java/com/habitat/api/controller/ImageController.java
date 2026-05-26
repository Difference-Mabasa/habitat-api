package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.service.PropertyImageService;
import com.habitat.api.service.UnitImageService;
import com.habitat.api.storage.StoredResource;
import com.habitat.api.storage.StorageService;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.constants.ErrorMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Public image serving + authenticated image lifecycle.
 *
 * <p>{@code GET /api/v1/files/images/{folder}/{filename}} is the
 * unauthenticated read path — property + unit photos appear on
 * /browse and /property/{id} for anonymous visitors. The folder
 * whitelist (properties, units) plus
 * {@link StorageService#open}'s path-traversal guard keeps the
 * surface tight; documents and leases stay behind the
 * ownership-checked endpoints from Phase 7.
 *
 * <p>{@code DELETE} is on the image rows themselves, scoped by
 * property edit-rights.
 */
@RestController
@RequiredArgsConstructor
public class ImageController {

    private static final java.util.Set<String> PUBLIC_FOLDERS =
            java.util.Set.of("properties", "units");

    private final StorageService storage;
    private final PropertyImageService propertyImages;
    private final UnitImageService unitImages;

    @GetMapping(ApiRoutes.FILES + "/images/{folder}/{filename}")
    public ResponseEntity<StreamingResponseBody> serve(
            @PathVariable String folder,
            @PathVariable String filename
    ) {
        if (!PUBLIC_FOLDERS.contains(folder)) {
            throw new ResourceNotFoundException(ErrorMessages.INVALID_FILE_PATH);
        }
        StoredResource resource = storage.open(folder + "/" + filename);
        StreamingResponseBody body = out -> {
            try (InputStream in = resource.content()) {
                in.transferTo(out);
            } catch (IOException e) {
                throw new IOException("stream copy failed", e);
            }
        };
        String mime = resource.mimeType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : resource.mimeType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mime)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(resource.size()))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(body);
    }

    @DeleteMapping(ApiRoutes.PROPERTY_IMAGES + "/{id}")
    public ResponseEntity<Void> deletePropertyImage(@PathVariable UUID id) {
        propertyImages.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(ApiRoutes.UNIT_IMAGES + "/{id}")
    public ResponseEntity<Void> deleteUnitImage(@PathVariable UUID id) {
        unitImages.delete(id);
        return ResponseEntity.noContent().build();
    }
}

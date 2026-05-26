package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.service.FileService;
import com.habitat.api.service.FileService.DownloadHandle;
import com.habitat.api.storage.StoredResource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Ownership-checked download endpoints. Sensitive directories (currently
 * just {@code documents}) are NEVER mapped to a static file handler;
 * everything flows through here.
 */
@RestController
@RequestMapping(ApiRoutes.FILES)
@RequiredArgsConstructor
public class FileController {

    private final FileService files;

    @GetMapping("/documents/{applicationId}/{docId}")
    public ResponseEntity<StreamingResponseBody> downloadApplicationDocument(
            @PathVariable UUID applicationId,
            @PathVariable UUID docId
    ) {
        DownloadHandle handle = files.openApplicationDocument(applicationId, docId);
        StoredResource resource = handle.resource();

        StreamingResponseBody body = out -> {
            try (InputStream in = resource.content()) {
                in.transferTo(out);
            } catch (IOException e) {
                throw new IOException("stream copy failed", e);
            }
        };

        String mime = resource.mimeType() != null
                ? resource.mimeType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mime)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(resource.size()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + handle.fileName().replace("\"", "_") + "\"")
                .body(body);
    }
}

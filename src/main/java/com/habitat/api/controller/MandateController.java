package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.mandate.IssueMandateRequest;
import com.habitat.api.dto.mandate.MandateResponse;
import com.habitat.api.service.BrowserRendererService;
import com.habitat.api.service.MandateService;
import com.habitat.api.storage.StoredResource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Mandate endpoints, scoped to a property. Authentication-gated and
 * limited to the property's manager / landlord / admin via
 * {@code PropertyService.requireCanEdit}.
 */
@RestController
@RequestMapping(ApiRoutes.PROPERTIES + "/{propertyId}/mandate")
@RequiredArgsConstructor
public class MandateController {

    private final MandateService mandates;
    private final BrowserRendererService browserRenderer;

    /**
     * Current mandate row for the property. Returns 404 (mapped from
     * the empty Optional) when none exists — the wizard treats that
     * as "no mandate issued yet" rather than an error.
     */
    @GetMapping
    public ResponseEntity<MandateResponse> get(@PathVariable UUID propertyId) {
        return mandates.getForProperty(propertyId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Issue a fresh mandate. Body carries type, fee, and landlord identity. */
    @PutMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MandateResponse issue(
            @PathVariable UUID propertyId,
            @Valid @RequestBody IssueMandateRequest req
    ) {
        return mandates.issue(propertyId, req);
    }

    /**
     * Email-to-landlord stub. Returns 204 — the agent's UI shows a
     * success toast. Real email delivery lands in Phase 8.
     */
    @PostMapping("/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void emailToLandlord(@PathVariable UUID propertyId) {
        mandates.emailToLandlord(propertyId);
    }

    /** Upload the offline-signed mandate PDF. */
    @PostMapping(value = "/signed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MandateResponse uploadSigned(
            @PathVariable UUID propertyId,
            @RequestPart("file") MultipartFile file
    ) {
        return mandates.uploadSigned(propertyId, file);
    }

    /**
     * Download the generated mandate PDF. Rendered on-demand via
     * headless Chromium against the React {@code /print/mandate/:id}
     * route — the on-screen design IS the document, so the PDF tracks
     * the latest mandate state (status, attestation, sigs) without a
     * regenerate-and-store step.
     */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable UUID propertyId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        mandates.requirePdfReadable(propertyId);
        byte[] pdf = browserRenderer.renderUrlToPdf("/print/mandate/" + propertyId, authHeader);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"mandate-" + propertyId + ".pdf\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf);
    }

    /** Download the signed mandate PDF (offline flow). */
    @GetMapping("/signed")
    public ResponseEntity<StreamingResponseBody> downloadSigned(@PathVariable UUID propertyId) {
        StoredResource r = mandates.openSignedPdf(propertyId);
        return streamPdf(r, "mandate-signed-" + propertyId + ".pdf");
    }

    private static ResponseEntity<StreamingResponseBody> streamPdf(StoredResource resource, String filename) {
        StreamingResponseBody body = out -> {
            try (InputStream in = resource.content()) {
                in.transferTo(out);
            } catch (IOException e) {
                throw new IOException("stream copy failed", e);
            }
        };
        String mime = resource.mimeType() == null
                ? MediaType.APPLICATION_PDF_VALUE
                : resource.mimeType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mime)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(resource.size()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + filename.replace("\"", "_") + "\"")
                .body(body);
    }
}

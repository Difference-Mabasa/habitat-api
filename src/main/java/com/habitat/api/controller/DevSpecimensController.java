package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.service.DevSpecimensService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Streams the mandate + lease PDF specimens the dev hub embeds. Thin
 * delegator — the synthetic-data construction sits in
 * {@link DevSpecimensService} because ArchUnit forbids the controller
 * package from touching {@code entity}.
 *
 * <p>{@code @Profile("!prod")} excludes both controller and service from
 * the production classpath.
 */
@RestController
@RequestMapping(ApiRoutes.DEV_SPECIMENS)
@RequiredArgsConstructor
@Profile("!prod")
public class DevSpecimensController {

    private final DevSpecimensService specimens;

    @GetMapping(value = "/mandate.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> mandateSpecimen() {
        return pdfResponse(specimens.renderMandateSpecimen(), "mandate-specimen.pdf");
    }

    @GetMapping(value = "/lease.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> leaseSpecimen() {
        return pdfResponse(specimens.renderLeaseSpecimen(), "lease-specimen.pdf");
    }

    @GetMapping(value = "/invoice.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> invoiceSpecimen() {
        return pdfResponse(specimens.renderInvoiceSpecimen(), "invoice-specimen.pdf");
    }

    private static ResponseEntity<byte[]> pdfResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(bytes);
    }
}

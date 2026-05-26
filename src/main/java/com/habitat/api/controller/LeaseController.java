package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.lease.DeclineLeaseRequest;
import com.habitat.api.dto.lease.IssueOtpResponse;
import com.habitat.api.dto.lease.LeaseResponse;
import com.habitat.api.dto.lease.SignLeaseRequest;
import com.habitat.api.service.LeaseService;
import com.habitat.api.service.LeaseService.PdfHandle;
import com.habitat.api.storage.StoredResource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Lease endpoints. Sits on {@code /leases}. Both tenant and landlord
 * read + sign here — role gating is enforced inside the service.
 */
@RestController
@RequestMapping(ApiRoutes.LEASES)
@RequiredArgsConstructor
public class LeaseController {

    private final LeaseService leases;

    @GetMapping("/me")
    public List<LeaseResponse> listMine() {
        return leases.listForTenant();
    }

    @GetMapping("/inbound")
    public List<LeaseResponse> listInbound() {
        return leases.listForLandlord();
    }

    @GetMapping("/{id}")
    public LeaseResponse get(@PathVariable UUID id) {
        return leases.getById(id);
    }

    /**
     * Issue a fresh OTP for the caller against this lease. Returns the
     * code inline as {@code devCode} until Phase 8 wires email delivery.
     */
    @PostMapping("/{id}/otp")
    public IssueOtpResponse issueOtp(@PathVariable UUID id) {
        return leases.issueSignOtp(id);
    }

    @PostMapping("/{id}/sign")
    public LeaseResponse sign(
            @PathVariable UUID id,
            @Valid @RequestBody SignLeaseRequest req
    ) {
        return leases.sign(id, req);
    }

    @PostMapping("/{id}/decline")
    public LeaseResponse decline(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) DeclineLeaseRequest req
    ) {
        return leases.decline(id, req);
    }

    /**
     * Download the signed-lease PDF. 409 LEASE_PDF_NOT_READY until both
     * parties have signed; 403 to non-parties.
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<StreamingResponseBody> downloadPdf(@PathVariable UUID id) {
        PdfHandle handle = leases.openSignedPdf(id);
        StoredResource resource = handle.resource();
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
                        "inline; filename=\"" + handle.fileName().replace("\"", "_") + "\"")
                .body(body);
    }
}

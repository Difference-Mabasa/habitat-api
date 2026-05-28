package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.PageResponse;
import com.habitat.api.dto.mandate.ApproveMandateRequest;
import com.habitat.api.dto.mandate.MandateResponse;
import com.habitat.api.dto.mandate.RejectMandateRequest;
import com.habitat.api.dto.mandate.RequestChangesRequest;
import com.habitat.api.dto.mandate.ResubmitMandateRequest;
import com.habitat.api.dto.mandate.WithdrawMandateRequest;
import com.habitat.api.enums.MandateStatus;
import com.habitat.api.service.MandateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Cross-property + mandate-scoped endpoints:
 *
 * <ul>
 *   <li>Inbox-style lists ({@code /awaiting-my-approval}, {@code /mine})
 *       that aren't naturally scoped to a single property.</li>
 *   <li>Action endpoints on a specific mandate ({@code /{mandateId}/approve},
 *       {@code /reject}, {@code /request-changes}, {@code /resubmit},
 *       {@code /withdraw}, {@code /email}, {@code /signed}). The caller
 *       already knows which mandate they're acting on — passing the id
 *       directly removes the "find the current one for this property"
 *       guesswork that was correct only by V41 invariant.</li>
 * </ul>
 *
 * <p>Property-scoped views ({@code GET /properties/{id}/mandate},
 * {@code PUT issue}, {@code /history}, {@code /pdf}, {@code /signed}
 * download) live on {@link MandateController}.
 */
@RestController
@RequestMapping(ApiRoutes.MANDATES)
@RequiredArgsConstructor
public class MandatesController {

    private final MandateService mandates;

    // ── Inbox-style lists ────────────────────────────────────────────

    /**
     * Mandates pending the caller's landlord-side approval. Powers
     * /mandate-approvals — the CTA target on
     * MANDATE_PENDING_LANDLORD_APPROVAL notifications. Paginated;
     * page size capped at 100 by the global PageSizeFilter.
     */
    @GetMapping("/awaiting-my-approval")
    public PageResponse<MandateResponse> awaitingMyApproval(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return mandates.listAwaitingMyApproval(page, size);
    }

    /**
     * Slice 4: agent inbox — every mandate they issued, optionally
     * filtered by status. Drives /my-mandates with status chips
     * including CHANGES_REQUESTED. Paginated; size capped at 100.
     */
    @GetMapping("/mine")
    public PageResponse<MandateResponse> mine(
            @RequestParam(required = false) MandateStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return mandates.listMineAsAgent(status, page, size);
    }

    // ── Mandate-scoped action endpoints ──────────────────────────────

    /**
     * Online landlord approves + e-signs the mandate. Body carries the
     * typed name; the service validates it matches the registered
     * profile before flipping status to ACTIVE. 403 unless the caller
     * is the resolved owner User on the mandate's property.landlord.
     */
    @PostMapping("/{mandateId}/approve")
    public MandateResponse approve(
            @PathVariable UUID mandateId,
            @Valid @RequestBody ApproveMandateRequest req
    ) {
        return mandates.approveByLandlord(mandateId, req);
    }

    /**
     * Online landlord rejects the mandate with a reason. Same
     * authorization as approve. The DTO enforces a 4–1000 char reason.
     */
    @PostMapping("/{mandateId}/reject")
    public MandateResponse reject(
            @PathVariable UUID mandateId,
            @Valid @RequestBody RejectMandateRequest req
    ) {
        return mandates.rejectByLandlord(mandateId, req);
    }

    /**
     * Slice 4: online landlord opens a structured change request
     * before signing. Mandate flips to CHANGES_REQUESTED until the
     * agent resubmits or withdraws.
     */
    @PostMapping("/{mandateId}/request-changes")
    public MandateResponse requestChanges(
            @PathVariable UUID mandateId,
            @Valid @RequestBody RequestChangesRequest req
    ) {
        return mandates.requestChanges(mandateId, req);
    }

    /**
     * Slice 4: agent applies a selective patch to a CHANGES_REQUESTED
     * mandate and resubmits for the landlord's approval. Marks open
     * change requests as ADDRESSED.
     */
    @PostMapping("/{mandateId}/resubmit")
    public MandateResponse resubmit(
            @PathVariable UUID mandateId,
            @Valid @RequestBody ResubmitMandateRequest req
    ) {
        return mandates.resubmit(mandateId, req);
    }

    /**
     * Slice 4: agent withdraws a pending mandate. Terminal — moves
     * the row to REJECTED with the agent recorded as withdrawer.
     */
    @PostMapping("/{mandateId}/withdraw")
    public MandateResponse withdraw(
            @PathVariable UUID mandateId,
            @Valid @RequestBody WithdrawMandateRequest req
    ) {
        return mandates.withdraw(mandateId, req);
    }

    /**
     * Email-to-landlord stub. Returns 204 — the agent's UI shows a
     * success toast. Real email delivery lands in Phase 8.
     */
    @PostMapping("/{mandateId}/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void emailToLandlord(@PathVariable UUID mandateId) {
        mandates.emailToLandlord(mandateId);
    }

    /** Upload the offline-signed mandate PDF for a specific mandate. */
    @PostMapping(value = "/{mandateId}/signed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MandateResponse uploadSigned(
            @PathVariable UUID mandateId,
            @RequestPart("file") MultipartFile file
    ) {
        return mandates.uploadSigned(mandateId, file);
    }
}

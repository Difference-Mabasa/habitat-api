package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.mandate.MandateResponse;
import com.habitat.api.service.MandateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cross-property mandate views — the inbox-style endpoints that
 * aren't naturally scoped to a single property. The property-scoped
 * lifecycle (issue / upload-signed / approve / reject / pdf / email)
 * lives on {@link MandateController}.
 */
@RestController
@RequestMapping(ApiRoutes.MANDATES)
@RequiredArgsConstructor
public class MandatesController {

    private final MandateService mandates;

    /**
     * Mandates pending the caller's landlord-side approval. Powers
     * /mandate-approvals — the CTA target on
     * MANDATE_PENDING_LANDLORD_APPROVAL notifications.
     */
    @GetMapping("/awaiting-my-approval")
    public List<MandateResponse> awaitingMyApproval() {
        return mandates.listAwaitingMyApproval();
    }
}

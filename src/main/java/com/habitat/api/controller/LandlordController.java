package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.landlord.LandlordLookupResponse;
import com.habitat.api.service.LandlordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated agent-facing landlord lookups. Authorization is
 * implicit — the endpoint is excluded from {@code PublicEndpoints},
 * so any signed-in user can hit it. We're intentionally permissive
 * here: an agent looking up an ID number sees only first/last name +
 * type + ownedByMe (no email/phone), which is enough to confirm a
 * match without leaking contact details across mandates.
 */
@RestController
@RequestMapping(ApiRoutes.LANDLORDS)
@RequiredArgsConstructor
public class LandlordController {

    private final LandlordService landlords;

    /**
     * Dedup lookup the wizard's mandate step calls before showing the
     * capture form. Returns {@code exists=false} when no row matches,
     * or a minimal identity card when one does. Throws 400 when the
     * id_number fails the SA ID Luhn check — guards against typo
     * collisions on the dedup key.
     */
    @GetMapping("/lookup")
    public LandlordLookupResponse lookup(@RequestParam("idNumber") String idNumber) {
        return landlords.lookupByIdNumber(idNumber);
    }
}

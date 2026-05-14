package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.lease.DeclineLeaseRequest;
import com.habitat.api.dto.lease.LeaseResponse;
import com.habitat.api.dto.lease.SignLeaseRequest;
import com.habitat.api.service.LeaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/{id}/sign")
    public LeaseResponse sign(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) SignLeaseRequest req
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
}

package com.habitat.api.dto.lease;

import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /leases/{id}/decline}. Tenant or landlord can
 * decline; the optional reason is carried back to the other party.
 */
public record DeclineLeaseRequest(
        @Size(max = 2000) String reason
) {}

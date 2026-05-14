package com.habitat.api.dto.application;

import com.habitat.api.enums.EmploymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request body for {@code POST /applications}. Mirrors backroom's
 * apply payload shape — everything bar {@code unitId} is optional so
 * a tenant can express interest with a single click.
 */
public record CreateApplicationRequest(
        @NotNull UUID unitId,

        /** Free-text introduction to the landlord. */
        @Size(max = 2000)
        String message,

        /** Tenant's preferred move-in date. The landlord may counter. */
        LocalDate moveInDate,

        /** Self-declared employment status. Soft hint, not gating. */
        EmploymentStatus employmentStatus
) {}

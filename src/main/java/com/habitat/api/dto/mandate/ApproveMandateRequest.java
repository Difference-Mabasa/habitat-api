package com.habitat.api.dto.mandate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /properties/{id}/mandate/approve}. The online
 * landlord types their full registered name as the e-signature; the
 * service normalises both sides (case + internal whitespace) and
 * rejects with {@link com.habitat.api.constants.ErrorMessages#MANDATE_SIGNED_NAME_MISMATCH}
 * if the typed name doesn't match the registered profile.
 *
 * <p>The 120-char ceiling matches the {@code signed_name} column width
 * on the {@code mandates} table (V36).
 */
public record ApproveMandateRequest(
        @NotBlank
        @Size(min = 2, max = 120)
        String signedName
) {}

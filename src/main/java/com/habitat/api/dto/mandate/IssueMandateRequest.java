package com.habitat.api.dto.mandate;

import com.habitat.api.enums.MandateType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Body for {@code POST /properties/{id}/mandate}.
 *
 * <p>The landlord identity is captured here at mandate-issue time
 * (rather than carried separately on the mandate row). The service
 * resolves it via {@code LandlordService.resolveForMandate}:
 *
 * <ul>
 *   <li>{@code landlordIdNumber} is the dedup key — when set and
 *       matching an existing {@code landlords} row, the mandate
 *       links to that row regardless of whether the captured
 *       contact details match. (Edits to existing landlord rows
 *       go through a separate, creator-only path.)</li>
 *   <li>When no row exists and {@code landlordEmail} resolves to a
 *       Habitat user, an ONLINE Landlord row is created.</li>
 *   <li>Otherwise an OFFLINE Landlord row is created with the
 *       captured first/last/email/phone, marking the calling
 *       agent as creator.</li>
 * </ul>
 */
public record IssueMandateRequest(
        @NotNull MandateType mandateType,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal feePercent,
        /** SA ID number — 13 digits, Luhn-validated. Optional for
         *  non-SA / passport-only landlords (no dedup in that case). */
        @Pattern(regexp = "^[0-9]{13}$", message = "SA ID number must be 13 digits.")
        @Size(max = 13) String landlordIdNumber,
        @Size(max = 100) String landlordFirstName,
        @Size(max = 100) String landlordLastName,
        @Email @Size(max = 255) String landlordEmail,
        @Size(max = 50) String landlordPhone,
        @Size(max = 2000) String notes
) {}

package com.habitat.api.dto.mandate;

import com.habitat.api.enums.MandateType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Body for {@code POST /properties/{id}/mandate}.
 *
 * <p>If {@code landlordEmail} resolves to a Habitat account, the
 * service issues an online mandate (status =
 * {@code PENDING_LANDLORD_APPROVAL}). Otherwise it issues an offline
 * mandate (status = {@code PENDING_OFFLINE_SIGNATURE}); the landlord
 * fields stored on the row are picked up by the generated PDF.
 */
public record IssueMandateRequest(
        @NotNull MandateType mandateType,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal feePercent,
        @Size(max = 200) String landlordName,
        @Email @Size(max = 255) String landlordEmail,
        @Size(max = 50) String landlordPhone,
        @Size(max = 2000) String notes
) {}

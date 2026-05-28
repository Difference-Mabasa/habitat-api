package com.habitat.api.dto.mandate;

import com.habitat.api.enums.MandateType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Body for {@code POST /properties/{id}/mandate/resubmit}. All fields
 * are optional; the agent supplies only the ones they're updating
 * (selective patch semantics). At least one must be non-null —
 * resubmitting with no changes is meaningless and returns 400.
 */
public record ResubmitMandateRequest(
        MandateType mandateType,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal feePercent,
        @Size(max = 2000) String notes
) {
    @AssertTrue(message = "Supply at least one field to update.")
    public boolean isNotEmpty() {
        return mandateType != null || feePercent != null || notes != null;
    }
}

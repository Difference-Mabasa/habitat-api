package com.habitat.api.dto.mandate;

import com.habitat.api.enums.ChangeRequestField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One typed item inside a {@link RequestChangesRequest}. The landlord
 * picks a field and supplies the new value; the service snapshots the
 * current value server-side so the historical record is trustworthy.
 */
public record ChangeItemRequest(
        @NotNull ChangeRequestField field,
        @NotBlank @Size(max = 1000) String requestedValue
) {}

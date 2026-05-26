package com.habitat.api.dto.property;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Body for {@code PUT /properties/{id}/amenities}. Full-list replace.
 * Empty list is allowed (clears the set).
 */
public record SetAmenitiesRequest(
        @NotNull List<UUID> amenityIds
) {}

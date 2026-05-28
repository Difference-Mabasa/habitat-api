package com.habitat.api.entity;

import com.habitat.api.enums.ChangeRequestField;

/**
 * Value object inside {@link MandateChangeRequest#getItems()}.
 * Persisted as jsonb via {@code @JdbcTypeCode(SqlTypes.JSON)} on the
 * containing list, so this stays a plain record — no JPA annotations.
 *
 * <p>{@code currentValue} is snapshotted server-side at request time
 * so the historical record reflects the mandate's actual state then,
 * not whatever the client thought it was. {@code requestedValue} is
 * what the landlord wants instead.
 */
public record ChangeItem(
        ChangeRequestField field,
        String currentValue,
        String requestedValue
) {}

package com.habitat.api.enums;

/**
 * Lifecycle state of a tenant {@code Viewing} request against a Unit.
 *
 * <p>{@code REQUESTED} → {@code APPROVED | REJECTED | CANCELLED} from
 * the tenant's side; {@code APPROVED} can later move to
 * {@code CANCELLED} (either party) or {@code COMPLETED} (the slot
 * came and went). REJECTED / CANCELLED / COMPLETED are terminal.
 *
 * <p>Stored as STRING on {@code viewings.status}.
 */
public enum ViewingStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    CANCELLED,
    COMPLETED
}

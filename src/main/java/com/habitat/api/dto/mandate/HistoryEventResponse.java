package com.habitat.api.dto.mandate;

import com.habitat.api.enums.HistoryEventKind;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * One row on the mandate history timeline. {@code payload} carries
 * round-specific data the UI renders inline (e.g. for CHANGES_REQUESTED
 * the items + comment; for APPROVED the typed signed name; for REJECTED
 * the reason). Keeping it as a generic map avoids a fan-out of subtypes
 * for a read-only audit surface.
 */
public record HistoryEventResponse(
        HistoryEventKind kind,
        OffsetDateTime at,
        UUID byUserId,
        String byUserName,
        Map<String, Object> payload
) {}

package com.habitat.api.dto.mandate;

import java.util.List;

/**
 * Returned by {@code GET /properties/{id}/mandate/history}.
 * Events are ordered oldest-first for natural top-down rendering.
 */
public record MandateHistoryResponse(List<HistoryEventResponse> events) {}

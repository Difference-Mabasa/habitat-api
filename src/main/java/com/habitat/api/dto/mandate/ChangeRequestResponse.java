package com.habitat.api.dto.mandate;

import com.habitat.api.entity.MandateChangeRequest;
import com.habitat.api.entity.User;
import com.habitat.api.enums.ChangeRequestStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Wire shape for a single change request. Embedded in
 * {@link MandateResponse#latestChangeRequest()} for the most recent
 * OPEN one, and returned in bulk from {@code GET /mandate/history}.
 */
public record ChangeRequestResponse(
        UUID id,
        OffsetDateTime requestedAt,
        UUID requestedByUserId,
        String requestedByUserName,
        String comment,
        List<ChangeItemResponse> items,
        ChangeRequestStatus status,
        OffsetDateTime resolvedAt,
        UUID resolvedByUserId,
        String resolvedByUserName
) {
    public static ChangeRequestResponse from(MandateChangeRequest cr) {
        return new ChangeRequestResponse(
                cr.getId(),
                cr.getRequestedAt(),
                cr.getRequestedByUser() == null ? null : cr.getRequestedByUser().getId(),
                displayName(cr.getRequestedByUser()),
                cr.getComment(),
                cr.getItems() == null
                        ? List.of()
                        : cr.getItems().stream().map(ChangeItemResponse::from).toList(),
                cr.getStatus(),
                cr.getResolvedAt(),
                cr.getResolvedByUser() == null ? null : cr.getResolvedByUser().getId(),
                displayName(cr.getResolvedByUser())
        );
    }

    private static String displayName(User u) {
        if (u == null) return null;
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getSurname() == null ? "" : u.getSurname();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? u.getEmail() : name;
    }
}

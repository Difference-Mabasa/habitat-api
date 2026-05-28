package com.habitat.api.dto.mandate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body for {@code POST /properties/{id}/mandate/request-changes}.
 * The landlord may submit either structured items (Fee / Scope /
 * Notes) or a freeform comment — but not both empty. The 4-char
 * comment floor mirrors slice 3's reject reason.
 */
public record RequestChangesRequest(
        @Valid List<ChangeItemRequest> items,
        @Size(max = 2000) String comment
) {
    /** Either at least one structured item OR a ≥ 4-char comment. */
    @AssertTrue(message = "Provide at least one change item or a comment.")
    public boolean isNotEmpty() {
        boolean hasItems = items != null && !items.isEmpty();
        boolean hasComment = comment != null && comment.trim().length() >= 4;
        return hasItems || hasComment;
    }
}

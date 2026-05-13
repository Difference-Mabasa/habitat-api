package com.habitat.api.enums;

/**
 * Lifecycle of a property listing.
 *
 * <ul>
 *   <li>{@link #DRAFT} — created but not visible to the public. Default
 *       on POST /properties. Promoted to LISTED via a separate publish
 *       call (deferred slice) once minimum criteria are met.</li>
 *   <li>{@link #LISTED} — visible in {@code GET /properties} search.</li>
 *   <li>{@link #UNLISTED} — previously listed, hidden without deletion;
 *       can be re-listed.</li>
 * </ul>
 */
public enum PropertyStatus {
    DRAFT,
    LISTED,
    UNLISTED
}

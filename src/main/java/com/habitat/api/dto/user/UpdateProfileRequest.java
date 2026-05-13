package com.habitat.api.dto.user;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Partial update for {@code PATCH /users/me}.
 *
 * Every field is optional. Null = "don't change this column". The Profile
 * screen sends only the field group the user just blurred out of, so
 * most requests touch 1–5 fields out of the dozen this shape can carry.
 *
 * To explicitly clear a value back to NULL on the row, send the field
 * as an empty string for textual columns (the service treats blank as
 * null) or as an empty array for {@link #interests}. We don't model
 * an explicit "clear-this-field" sentinel — that's overkill for the
 * profile shape.
 */
public record UpdateProfileRequest(
        @Size(min = 1, max = 40) String firstName,
        @Size(min = 1, max = 40) String surname,
        @Size(max = 20) String phone,
        @Size(max = 500) String bio,
        List<@Size(max = 60) String> interests,
        @Size(max = 120) String jobTitle,
        @Size(max = 120) String employer,
        @Size(max = 255) String education,
        @Size(max = 255) String addressLine,
        @Size(max = 120) String suburb,
        @Size(max = 120) String city,
        @Size(max = 80) String province,
        @Size(max = 10) String postalCode,
        Double latitude,
        Double longitude,
        @Size(max = 80) String area
) {}

package com.habitat.api.dto.landlord;

import com.habitat.api.entity.Landlord;
import com.habitat.api.enums.LandlordType;

import java.util.UUID;

/**
 * Minimal landlord identity for embedding in other responses
 * ({@code PropertyDetailResponse.owner}, mandate views). Contact
 * fields are resolved server-side so the UI doesn't switch on type to
 * find the right source — for ONLINE rows they come through the
 * linked user, for OFFLINE rows they come from the captured
 * fields. The raw type is still surfaced so the UI can label the
 * landlord ("registered" / "offline").
 *
 * <p>{@code userId} is non-null only when {@code type = ONLINE}, so
 * callers that need to deep-link to a user profile can do so without
 * a second lookup.
 */
public record LandlordRef(
        UUID id,
        LandlordType type,
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String idNumber
) {
    public static LandlordRef from(Landlord l) {
        if (l == null) return null;
        boolean online = l.getType() == LandlordType.ONLINE && l.getUser() != null;
        return new LandlordRef(
                l.getId(),
                l.getType(),
                online ? l.getUser().getId() : null,
                online ? l.getUser().getFirstName() : l.getFirstName(),
                online ? l.getUser().getSurname() : l.getLastName(),
                online ? l.getUser().getEmail() : l.getEmail(),
                online ? l.getUser().getPhone() : l.getPhone(),
                l.getIdNumber()
        );
    }
}

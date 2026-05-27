package com.habitat.api.dto.landlord;

import com.habitat.api.entity.Landlord;
import com.habitat.api.enums.LandlordType;
import com.habitat.api.security.SecurityUtils;

import java.util.UUID;

/**
 * Response shape for {@code GET /landlords/lookup?idNumber=...} —
 * minimal so the wizard can decide whether to render a "link to
 * existing" branch or a fresh capture form without leaking contact
 * details across mandates.
 *
 * <p>{@code firstName}/{@code lastName} are surfaced so the agent can
 * eyeball the match ("yes, this is who I'm capturing"). Email and
 * phone are intentionally omitted — the agent is expected to confirm
 * those out-of-band with the landlord at capture time. {@code
 * ownedByMe} tells the UI whether the current agent is the row's
 * creator and therefore has edit rights.
 */
public record LandlordLookupResponse(
        boolean exists,
        UUID id,
        LandlordType type,
        boolean hasUserAccount,
        String firstName,
        String lastName,
        boolean ownedByMe
) {
    public static LandlordLookupResponse notFound() {
        return new LandlordLookupResponse(false, null, null, false, null, null, false);
    }

    public static LandlordLookupResponse from(Landlord l, SecurityUtils security) {
        if (l == null) return notFound();
        UUID me = security.currentUserId().orElse(null);
        boolean ownedByMe =
                l.getType() == LandlordType.OFFLINE
                        && l.getCreatedByAgent() != null
                        && l.getCreatedByAgent().getId().equals(me);
        boolean online = l.getType() == LandlordType.ONLINE && l.getUser() != null;
        return new LandlordLookupResponse(
                true,
                l.getId(),
                l.getType(),
                online,
                online ? l.getUser().getFirstName() : l.getFirstName(),
                online ? l.getUser().getSurname() : l.getLastName(),
                ownedByMe
        );
    }
}

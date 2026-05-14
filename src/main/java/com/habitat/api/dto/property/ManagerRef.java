package com.habitat.api.dto.property;

import com.habitat.api.entity.User;

import java.util.UUID;

/**
 * Minimal landlord/manager identity attached to a property detail
 * response — just enough for the "Listed by" card on /property/:id.
 * Keeps PII surface narrow: no email, no phone, no roles.
 */
public record ManagerRef(
        UUID id,
        String firstName,
        String surname
) {
    public static ManagerRef from(User u) {
        if (u == null) return null;
        return new ManagerRef(u.getId(), u.getFirstName(), u.getSurname());
    }
}

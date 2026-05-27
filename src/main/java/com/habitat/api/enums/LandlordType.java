package com.habitat.api.enums;

/**
 * Whether a {@link com.habitat.api.entity.Landlord} is backed by a
 * registered Habitat {@code User} account.
 *
 * <p>{@code ONLINE} — landlord has a Habitat account. {@code user_id}
 * is set; name / email / phone are read through that user.
 *
 * <p>{@code OFFLINE} — landlord is not on Habitat. The agent who
 * issued the mandate captured the contact details directly on the
 * landlord row. When the offline landlord later signs up with the
 * same SA ID number we flip the row to {@code ONLINE} and clear the
 * captured contact fields.
 */
public enum LandlordType {
    ONLINE,
    OFFLINE
}

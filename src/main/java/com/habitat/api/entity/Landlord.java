package com.habitat.api.entity;

import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.LandlordType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * The legal owner identity attached to a {@link Property}. Distinct
 * from {@link User} so we can represent owners who don't (yet) have a
 * Habitat account — offline landlords captured by an agent during the
 * mandate flow.
 *
 * <p>Invariants enforced at the DB layer (V30):
 * <ul>
 *   <li>{@code type = ONLINE} ⇒ {@code user} non-null,
 *       contact fields (firstName, lastName, email, phone) null —
 *       read through {@code user}.</li>
 *   <li>{@code type = OFFLINE} ⇒ {@code user} null,
 *       {@code createdByAgent} non-null, contact fields populated.</li>
 *   <li>{@code user_id} is UNIQUE among non-null rows — one Landlord
 *       per User.</li>
 *   <li>{@code id_number} is UNIQUE among non-null rows — find-or-create
 *       on SA ID is race-safe.</li>
 * </ul>
 *
 * <p>Edit rights on an OFFLINE row belong to {@code createdByAgent}
 * exclusively. Other agents linking via the dedup lookup can read but
 * not write — they submit change requests (Phase 2 flow) instead.
 * Once a row flips to ONLINE the {@code user} edits via account
 * settings and the creator-agent loses write access.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "landlords")
@SQLRestriction("deleted_at IS NULL")
public class Landlord extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private LandlordType type;

    /**
     * Habitat user behind this landlord. NOT NULL when {@code type =
     * ONLINE}; NULL when OFFLINE. UNIQUE among non-null rows so one
     * User maps to at most one Landlord.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * SA ID number — 13 digits, Luhn-validated at the API boundary.
     * Optional (offline foreign / passport-only landlords leave it
     * null), UNIQUE among non-null rows so a find-or-create lookup
     * dedupes across agents. Matched against on offline → online
     * auto-link when an owner later signs up with the same ID.
     */
    @Column(name = "id_number", length = 13)
    private String idNumber;

    /**
     * Agent who first captured an OFFLINE landlord. Owns write rights
     * on the row — other agents that link via the SA-ID dedup lookup
     * read but can only request changes via the Phase 2 inbox flow.
     * NULL for rows born ONLINE (LANDLORD_DIRECT or a User signing up
     * cleanly).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_agent_id")
    private User createdByAgent;

    /** Captured by the agent for OFFLINE rows; NULL when ONLINE. */
    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    /** Convenience — full name for ONLINE reads through user, for
     *  OFFLINE assembles from captured first/last. */
    public String displayName() {
        if (type == LandlordType.ONLINE && user != null) {
            String first = user.getFirstName() == null ? "" : user.getFirstName();
            String last = user.getSurname() == null ? "" : user.getSurname();
            String name = (first + " " + last).trim();
            return name.isEmpty() ? user.getEmail() : name;
        }
        String first = firstName == null ? "" : firstName;
        String last = lastName == null ? "" : lastName;
        String name = (first + " " + last).trim();
        return name.isEmpty() ? (email == null ? "Offline landlord" : email) : name;
    }

    /** Email source-of-truth — through user when ONLINE. */
    public String resolvedEmail() {
        if (type == LandlordType.ONLINE && user != null) return user.getEmail();
        return email;
    }
}

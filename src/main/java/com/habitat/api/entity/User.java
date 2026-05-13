package com.habitat.api.entity;

import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 40)
    private String firstName;

    @Column(name = "surname", nullable = false, length = 40)
    private String surname;

    /**
     * All roles this user can act as. Driven by ADMIN's ability to grant
     * multiple workspaces.
     */
    @ElementCollection(fetch = FetchType.EAGER, targetClass = Role.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /** Last role the user actively used. Drives which dashboard renders. */
    @Enumerated(EnumType.STRING)
    @Column(name = "active_role", nullable = false)
    private Role activeRole;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    /** Default area / hood — used for the Feed's "Local" sub-tab. */
    @Column(name = "area")
    private String area;

    // ── Profile-edit fields (V7) ───────────────────────────────────────
    // All nullable. The Profile screen autosaves these per field group
    // via PATCH /users/me; null = field hasn't been provided yet, not a
    // request to clear (the PATCH request uses null-vs-absent to
    // distinguish intent at the DTO layer).

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    /**
     * Free-form tag set. Backed by a Postgres TEXT[] for proper array
     * semantics — Hibernate 6 handles this directly via @JdbcTypeCode.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "interests", columnDefinition = "TEXT[]")
    private List<String> interests;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @Column(name = "employer", length = 120)
    private String employer;

    @Column(name = "education", length = 255)
    private String education;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(name = "suburb", length = 120)
    private String suburb;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "province", length = 80)
    private String province;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    public UUID getUserId() {
        return getId();
    }
}

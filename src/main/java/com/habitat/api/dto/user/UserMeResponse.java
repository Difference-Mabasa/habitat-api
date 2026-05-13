package com.habitat.api.dto.user;

import com.habitat.api.entity.User;
import com.habitat.api.enums.Role;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Public-facing shape of the signed-in user. Never include the password
 * hash, refresh token, or anything else that could compromise the account.
 */
@Builder
public record UserMeResponse(
        UUID id,
        String email,
        String firstName,
        String surname,
        Set<Role> roles,
        Role activeRole,
        boolean emailVerified,
        String area,
        OffsetDateTime createdAt,
        // Profile-edit fields (V7). Nullable on the wire whenever the
        // user hasn't filled them in yet.
        String phone,
        String bio,
        List<String> interests,
        String jobTitle,
        String employer,
        String education,
        String addressLine,
        String suburb,
        String city,
        String province,
        String postalCode,
        Double latitude,
        Double longitude
) {
    public static UserMeResponse from(User user) {
        return UserMeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .surname(user.getSurname())
                .roles(Set.copyOf(user.getRoles()))
                .activeRole(user.getActiveRole())
                .emailVerified(user.isEmailVerified())
                .area(user.getArea())
                .createdAt(user.getCreatedAt())
                .phone(user.getPhone())
                .bio(user.getBio())
                .interests(user.getInterests())
                .jobTitle(user.getJobTitle())
                .employer(user.getEmployer())
                .education(user.getEducation())
                .addressLine(user.getAddressLine())
                .suburb(user.getSuburb())
                .city(user.getCity())
                .province(user.getProvince())
                .postalCode(user.getPostalCode())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .build();
    }
}

package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.landlord.LandlordLookupResponse;
import com.habitat.api.entity.Landlord;
import com.habitat.api.entity.User;
import com.habitat.api.enums.LandlordType;
import com.habitat.api.exception.BadRequestException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.LandlordRepository;
import com.habitat.api.repository.UserRepository;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.util.SaIdNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Landlord identity lifecycle. Three entry points the rest of the
 * service layer uses:
 *
 * <ul>
 *   <li>{@link #lookupByIdNumber(String)} — read-only lookup the
 *       wizard's step-2 dedup UX calls. Returns minimal identity
 *       (first/last name, type, hasUserAccount, ownedByMe) so the
 *       agent can confirm match without leaking contact details
 *       across mandates.</li>
 *   <li>{@link #findOrCreateForUser(User)} — used by
 *       {@code PropertyService.create} when listing mode is
 *       LANDLORD_DIRECT (the caller is the owner). Returns the
 *       canonical ONLINE Landlord row keyed by user, creating it on
 *       first use.</li>
 *   <li>{@link #resolveForMandate(MandateLandlordCapture)} — used by
 *       {@code MandateService.issue}. Find-or-create by SA ID number;
 *       falls back to creating a new row when no match. Decides
 *       ONLINE vs OFFLINE by checking whether the captured email
 *       resolves to a registered Habitat user.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LandlordService {

    private final LandlordRepository landlords;
    private final UserRepository users;
    private final SecurityUtils security;

    /** Read-only dedup lookup the wizard hits before showing the capture form. */
    @Transactional(readOnly = true)
    public LandlordLookupResponse lookupByIdNumber(String idNumber) {
        if (idNumber == null || idNumber.isBlank()) {
            return LandlordLookupResponse.notFound();
        }
        if (!SaIdNumber.isValid(idNumber)) {
            throw new BadRequestException(ErrorMessages.LANDLORD_ID_NUMBER_INVALID);
        }
        return landlords.findByIdNumber(idNumber.trim())
                .map(l -> LandlordLookupResponse.from(l, security))
                .orElseGet(LandlordLookupResponse::notFound);
    }

    /**
     * Returns the canonical ONLINE Landlord row for {@code user},
     * creating it on first call. Used by LANDLORD_DIRECT property
     * creation — the caller is both manager and owner so the Landlord
     * row mirrors them.
     */
    @Transactional
    public Landlord findOrCreateForUser(User user) {
        return landlords.findByUser_Id(user.getId())
                .orElseGet(() -> landlords.save(Landlord.builder()
                        .type(LandlordType.ONLINE)
                        .user(user)
                        .build()));
    }

    /**
     * Find-or-create the Landlord row a mandate links to. Used by
     * {@code MandateService.issue}. Steps:
     *
     * <ol>
     *   <li>Validate id_number (Luhn) if present.</li>
     *   <li>Look up by id_number — if hit, return existing row
     *       regardless of type.</li>
     *   <li>If miss, look up Habitat user by email — if hit, create
     *       ONLINE row linked to that user (so when the user later
     *       owns more properties, they're all one landlord).</li>
     *   <li>Otherwise create OFFLINE row with the captured contact
     *       fields, marking the calling agent as creator.</li>
     * </ol>
     */
    @Transactional
    public Landlord resolveForMandate(MandateLandlordCapture capture) {
        if (capture.idNumber() != null && !capture.idNumber().isBlank()) {
            if (!SaIdNumber.isValid(capture.idNumber())) {
                throw new BadRequestException(ErrorMessages.LANDLORD_ID_NUMBER_INVALID);
            }
            var existing = landlords.findByIdNumber(capture.idNumber().trim());
            if (existing.isPresent()) {
                log.info("mandate landlord dedup hit on id_number → {}", existing.get().getId());
                return existing.get();
            }
        }

        if (capture.email() == null || capture.email().isBlank()) {
            throw new BadRequestException(ErrorMessages.LANDLORD_REQUIRED_FIELDS);
        }

        var matchedUser = users.findByEmailIgnoreCase(capture.email().trim());
        if (matchedUser.isPresent()) {
            // ONLINE branch — re-use any existing Landlord row for
            // this user so we don't create a duplicate when the same
            // person's been a landlord before.
            return findOrCreateForUser(matchedUser.get());
        }

        // No matching user. If the caller didn't send any of the
        // offline-capture fields (firstName / lastName / phone /
        // idNumber) we know they intended the online path — emit a
        // specific "email not on Habitat" error so they know to ask
        // the landlord to register or to switch to the offline flow,
        // rather than the generic offline-fields-missing message.
        boolean offlineCaptureProvided =
                (capture.firstName() != null && !capture.firstName().isBlank())
                || (capture.lastName() != null && !capture.lastName().isBlank())
                || (capture.idNumber() != null && !capture.idNumber().isBlank())
                || (capture.phone() != null && !capture.phone().isBlank());
        if (!offlineCaptureProvided) {
            throw new BadRequestException(ErrorMessages.LANDLORD_EMAIL_NOT_ON_HABITAT);
        }

        if (capture.firstName() == null || capture.firstName().isBlank()
                || capture.lastName() == null || capture.lastName().isBlank()) {
            throw new BadRequestException(ErrorMessages.LANDLORD_REQUIRED_FIELDS);
        }

        UUID agentId = security.requireUserId();
        User agent = users.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));

        return landlords.save(Landlord.builder()
                .type(LandlordType.OFFLINE)
                .createdByAgent(agent)
                .idNumber(capture.idNumber() == null || capture.idNumber().isBlank()
                        ? null
                        : capture.idNumber().trim())
                .firstName(capture.firstName().trim())
                .lastName(capture.lastName().trim())
                .email(capture.email().trim())
                .phone(capture.phone() == null || capture.phone().isBlank()
                        ? null
                        : capture.phone().trim())
                .build());
    }

    /**
     * Enforces creator-only edit on OFFLINE rows. ONLINE rows are
     * edited by the user via account settings, so the calling agent
     * is never the right writer there — throw ForbiddenException.
     */
    public void requireCanEditDetails(Landlord landlord) {
        if (security.isPrivileged()) return;
        UUID me = security.currentUserId().orElseThrow(() ->
                new ForbiddenException(ErrorMessages.AUTH_REQUIRED));
        if (landlord.getType() == LandlordType.ONLINE) {
            // Online landlords manage their own profile.
            throw new ForbiddenException(ErrorMessages.LANDLORD_EDIT_FORBIDDEN);
        }
        if (landlord.getCreatedByAgent() == null
                || !landlord.getCreatedByAgent().getId().equals(me)) {
            throw new ForbiddenException(ErrorMessages.LANDLORD_EDIT_FORBIDDEN);
        }
    }

    /**
     * Capture payload from the wizard's mandate step. A record rather
     * than the DTO directly so the service has no jakarta.validation
     * dependency on the request shape.
     */
    public record MandateLandlordCapture(
            String idNumber,
            String firstName,
            String lastName,
            String email,
            String phone
    ) {}
}

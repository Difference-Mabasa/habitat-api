package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.user.UpdateProfileRequest;
import com.habitat.api.dto.user.UserMeResponse;
import com.habitat.api.entity.User;
import com.habitat.api.enums.Role;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserMeResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));
        return UserMeResponse.from(user);
    }

    public UserMeResponse switchActiveRole(UUID userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));
        if (!user.getRoles().contains(role)) {
            throw new ForbiddenException(ErrorMessages.ROLE_NOT_OWNED);
        }
        if (user.getActiveRole() != role) {
            user.setActiveRole(role);
            userRepository.save(user);
            log.info("user {} switched active role to {}", userId, role);
        }
        return UserMeResponse.from(user);
    }

    /**
     * Apply a partial profile update. Only non-null fields on the request
     * touch the entity — null means "don't change this column". To clear
     * a textual column back to NULL the caller sends an empty string;
     * the helpers below normalise blank-as-null per column.
     *
     * Returns the freshly-merged UserMeResponse so the UI can refresh
     * without a separate GET.
     */
    public UserMeResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));

        if (req.firstName() != null)   user.setFirstName(req.firstName().trim());
        if (req.surname() != null)     user.setSurname(req.surname().trim());
        if (req.phone() != null)       user.setPhone(blankToNull(req.phone()));
        if (req.bio() != null)         user.setBio(blankToNull(req.bio()));
        if (req.interests() != null)   user.setInterests(req.interests().isEmpty() ? null : req.interests());
        if (req.jobTitle() != null)    user.setJobTitle(blankToNull(req.jobTitle()));
        if (req.employer() != null)    user.setEmployer(blankToNull(req.employer()));
        if (req.education() != null)   user.setEducation(blankToNull(req.education()));
        if (req.addressLine() != null) user.setAddressLine(blankToNull(req.addressLine()));
        if (req.suburb() != null)      user.setSuburb(blankToNull(req.suburb()));
        if (req.city() != null)        user.setCity(blankToNull(req.city()));
        if (req.province() != null)    user.setProvince(blankToNull(req.province()));
        if (req.postalCode() != null)  user.setPostalCode(blankToNull(req.postalCode()));
        if (req.latitude() != null)    user.setLatitude(req.latitude());
        if (req.longitude() != null)   user.setLongitude(req.longitude());
        if (req.area() != null)        user.setArea(blankToNull(req.area()));

        userRepository.save(user);
        log.info("user {} profile updated", userId);
        return UserMeResponse.from(user);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

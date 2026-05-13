package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.PageResponse;
import com.habitat.api.dto.property.CreatePropertyRequest;
import com.habitat.api.dto.property.CreateUnitRequest;
import com.habitat.api.dto.property.PropertyDetailResponse;
import com.habitat.api.dto.property.PropertySummary;
import com.habitat.api.dto.property.UnitResponse;
import com.habitat.api.dto.property.UpdatePropertyRequest;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.User;
import com.habitat.api.enums.PaymentFrequency;
import com.habitat.api.enums.PropertyStatus;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.enums.UnitType;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.PropertyRepository;
import com.habitat.api.repository.UnitRepository;
import com.habitat.api.repository.UserRepository;
import com.habitat.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Property + nested-unit business logic. The Property entity owns the
 * landlord/manager relationship and is the unit of authorisation: edits
 * to units route through this service so the manager check is consistent.
 *
 * Public reads scope to {@link PropertyStatus#LISTED} only; managers and
 * admins see their own properties at any status (returning the same 404
 * for non-listed-and-not-owned that other "resource hidden" paths do, to
 * avoid leaking the existence of draft listings).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyService {

    private final PropertyRepository properties;
    private final UnitRepository units;
    private final UserRepository users;
    private final SecurityUtils security;

    @Transactional(readOnly = true)
    public PageResponse<PropertySummary> search(
            List<String> locations,
            List<UnitType> unitTypes,
            BigDecimal maxPrice,
            Integer minBeds,
            int page,
            int size
    ) {
        // Multi-location filtering is done in Java after SQL fetches the
        // already-narrowed set by type/price/beds. JPQL doesn't compose OR
        // clauses over a variable-length list cleanly without either native
        // SQL (loses pagination support without a count query) or the
        // Specification API (heavier for the v1 catalogue). Sub-100 properties
        // makes in-memory filtering trivial; revisit if the catalogue grows.
        List<UnitType> typesParam = (unitTypes == null || unitTypes.isEmpty()) ? null : unitTypes;
        List<String> cleanLocations = locations == null
                ? List.of()
                : locations.stream()
                        .map(PropertyService::blankToNull)
                        .filter(s -> s != null)
                        .toList();

        Pageable big = PageRequest.of(0, MAX_FETCH);
        Page<Property> raw = properties.search(
                PropertyStatus.LISTED,
                "",
                typesParam,
                maxPrice,
                minBeds,
                big
        );

        List<Property> filtered = cleanLocations.isEmpty()
                ? raw.getContent()
                : raw.getContent().stream()
                        .filter(p -> matchesAnyLocation(p, cleanLocations))
                        .toList();

        int total = filtered.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<PropertySummary> content = filtered.subList(from, to).stream()
                .map(PropertySummary::from)
                .toList();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);

        return PageResponse.<PropertySummary>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages(totalPages)
                .build();
    }

    private static final int MAX_FETCH = 200;

    private static boolean matchesAnyLocation(Property p, List<String> locations) {
        return locations.stream().anyMatch(loc -> {
            String needle = loc.toLowerCase();
            return contains(p.getSuburb(), needle)
                    || contains(p.getCity(), needle)
                    || contains(p.getProvince(), needle);
        });
    }

    private static boolean contains(String haystack, String needleLower) {
        return haystack != null && haystack.toLowerCase().contains(needleLower);
    }

    @Transactional(readOnly = true)
    public PropertyDetailResponse getById(UUID id) {
        Property p = findOrThrow(id);
        if (p.getStatus() != PropertyStatus.LISTED && !canEdit(p)) {
            throw new ResourceNotFoundException(ErrorMessages.PROPERTY_NOT_FOUND);
        }
        return PropertyDetailResponse.from(p);
    }

    @Transactional
    public PropertyDetailResponse create(CreatePropertyRequest req) {
        UUID me = security.requireUserId();
        User user = users.findById(me)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));

        Property p = Property.builder()
                .landlord(user)
                .manager(user)
                .title(req.title().trim())
                .description(blankToNull(req.description()))
                .propertyType(req.propertyType())
                .status(PropertyStatus.DRAFT)
                .addressLine(blankToNull(req.addressLine()))
                .suburb(blankToNull(req.suburb()))
                .city(blankToNull(req.city()))
                .province(blankToNull(req.province()))
                .postalCode(blankToNull(req.postalCode()))
                .latitude(req.latitude())
                .longitude(req.longitude())
                .build();
        properties.save(p);
        log.info("property {} created by user {}", p.getId(), me);
        return PropertyDetailResponse.from(p);
    }

    @Transactional
    public PropertyDetailResponse update(UUID id, UpdatePropertyRequest req) {
        Property p = findOrThrow(id);
        requireCanEdit(p);

        if (req.title() != null)        p.setTitle(req.title().trim());
        if (req.description() != null)  p.setDescription(blankToNull(req.description()));
        if (req.propertyType() != null) p.setPropertyType(req.propertyType());
        if (req.status() != null)       p.setStatus(req.status());
        if (req.addressLine() != null)  p.setAddressLine(blankToNull(req.addressLine()));
        if (req.suburb() != null)       p.setSuburb(blankToNull(req.suburb()));
        if (req.city() != null)         p.setCity(blankToNull(req.city()));
        if (req.province() != null)     p.setProvince(blankToNull(req.province()));
        if (req.postalCode() != null)   p.setPostalCode(blankToNull(req.postalCode()));
        if (req.latitude() != null)     p.setLatitude(req.latitude());
        if (req.longitude() != null)    p.setLongitude(req.longitude());

        log.info("property {} updated by user {}", p.getId(), security.requireUserId());
        return PropertyDetailResponse.from(p);
    }

    @Transactional
    public void delete(UUID id) {
        Property p = findOrThrow(id);
        requireCanEdit(p);
        p.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        log.info("property {} deleted by user {}", p.getId(), security.requireUserId());
    }

    @Transactional
    public UnitResponse addUnit(UUID propertyId, CreateUnitRequest req) {
        Property p = findOrThrow(propertyId);
        requireCanEdit(p);
        Unit u = Unit.builder()
                .property(p)
                .unitNumber(blankToNull(req.unitNumber()))
                .title(req.title().trim())
                .description(blankToNull(req.description()))
                .unitType(req.unitType())
                .status(UnitStatus.AVAILABLE)
                .furnishing(req.furnishing())
                .price(req.price())
                .paymentFrequency(req.paymentFrequency() == null ? PaymentFrequency.MONTHLY : req.paymentFrequency())
                .deposit(req.deposit())
                .bedrooms(req.bedrooms())
                .bathrooms(req.bathrooms())
                .sqm(req.sqm())
                .maxOccupants(req.maxOccupants())
                .waterIncluded(Boolean.TRUE.equals(req.waterIncluded()))
                .electricityIncluded(Boolean.TRUE.equals(req.electricityIncluded()))
                .petsAllowed(Boolean.TRUE.equals(req.petsAllowed()))
                .availableFrom(req.availableFrom())
                .build();
        units.save(u);
        log.info("unit {} added to property {} by user {}", u.getId(), p.getId(), security.requireUserId());
        return UnitResponse.from(u);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Property findOrThrow(UUID id) {
        return properties.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.PROPERTY_NOT_FOUND));
    }

    /** Can the current caller edit this property? Admins always can. */
    public boolean canEdit(Property p) {
        if (security.isPrivileged()) return true;
        UUID me = security.currentUserId().orElse(null);
        if (me == null) return false;
        boolean isManager = p.getManager() != null && me.equals(p.getManager().getId());
        boolean isLandlord = p.getLandlord() != null && me.equals(p.getLandlord().getId());
        return isManager || isLandlord;
    }

    public void requireCanEdit(Property p) {
        if (!canEdit(p)) throw new ForbiddenException(ErrorMessages.FORBIDDEN);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

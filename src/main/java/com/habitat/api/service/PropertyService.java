package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.constants.LandingContent;
import com.habitat.api.dto.PageResponse;
import com.habitat.api.dto.property.CreatePropertyRequest;
import com.habitat.api.dto.property.CreateUnitRequest;
import com.habitat.api.dto.property.PopularAreaResponse;
import com.habitat.api.dto.property.PropertyDetailResponse;
import com.habitat.api.dto.property.PropertySummary;
import com.habitat.api.dto.property.UnitResponse;
import com.habitat.api.dto.property.UpdatePropertyRequest;
import com.habitat.api.entity.Amenity;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.PropertyRequiredDocument;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.User;
import com.habitat.api.enums.DocumentType;
import com.habitat.api.enums.ListingMode;
import com.habitat.api.enums.PaymentFrequency;
import com.habitat.api.enums.PropertyStatus;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.enums.UnitType;
import com.habitat.api.exception.BadRequestException;
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.AmenityRepository;
import com.habitat.api.repository.PropertyRepository;
import com.habitat.api.repository.PropertyRequiredDocumentRepository;
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
import java.util.Comparator;
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
    private final PropertyRequiredDocumentRepository requiredDocs;
    private final AmenityRepository amenities;
    private final SecurityUtils security;

    public enum SortKey {
        NEWEST, PRICE, BEDROOMS, SIZE
    }

    public enum SortDirection {
        ASC, DESC
    }

    @Transactional(readOnly = true)
    public PageResponse<PropertySummary> search(
            List<String> locations,
            List<UnitType> unitTypes,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minBeds,
            Integer minSqm,
            SortKey sortKey,
            SortDirection sortDirection,
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
                minPrice,
                maxPrice,
                minBeds,
                minSqm,
                big
        );

        List<Property> filtered = cleanLocations.isEmpty()
                ? raw.getContent()
                : raw.getContent().stream()
                        .filter(p -> matchesAnyLocation(p, cleanLocations))
                        .toList();

        // Sort in Java for the same reason as the location filter: the
        // headline-unit fields (cheapest unit price / bedrooms / sqm) live
        // on Unit, not Property, so SQL sorting would need a correlated
        // subquery and the catalogue is small enough that an in-memory
        // sort costs nothing measurable.
        List<Property> sorted = sortProperties(filtered, sortKey, sortDirection);

        int total = sorted.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<PropertySummary> content = sorted.subList(from, to).stream()
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

    /**
     * Order the filtered set by the requested key. NEWEST sorts on
     * {@code createdAt}; PRICE / BEDROOMS / SIZE all pull from the cheapest
     * available unit (the "headline" unit shown on a PropertyCard).
     * Properties without an available unit drop to the end of the list
     * regardless of direction so the gallery never shows blank cards.
     */
    private static List<Property> sortProperties(List<Property> input, SortKey key, SortDirection dir) {
        SortKey effectiveKey = key == null ? SortKey.NEWEST : key;
        SortDirection effectiveDir = dir == null
                ? (effectiveKey == SortKey.NEWEST ? SortDirection.DESC : SortDirection.ASC)
                : dir;

        Comparator<Property> base = switch (effectiveKey) {
            case NEWEST    -> Comparator.comparing(Property::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case PRICE     -> Comparator.comparing(p -> headlineBigDecimal(p, Unit::getPrice), Comparator.nullsLast(Comparator.naturalOrder()));
            case BEDROOMS  -> Comparator.comparing(p -> headlineInteger(p, Unit::getBedrooms), Comparator.nullsLast(Comparator.naturalOrder()));
            case SIZE      -> Comparator.comparing(p -> headlineInteger(p, Unit::getSqm), Comparator.nullsLast(Comparator.naturalOrder()));
        };
        Comparator<Property> ordered = effectiveDir == SortDirection.DESC ? base.reversed() : base;
        return input.stream().sorted(ordered).toList();
    }

    private static BigDecimal headlineBigDecimal(Property p, java.util.function.Function<Unit, BigDecimal> pick) {
        return p.getUnits().stream()
                .filter(u -> u.getStatus() == UnitStatus.AVAILABLE)
                .map(pick)
                .filter(v -> v != null)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    private static Integer headlineInteger(Property p, java.util.function.Function<Unit, Integer> pick) {
        return p.getUnits().stream()
                .filter(u -> u.getStatus() == UnitStatus.AVAILABLE)
                .map(pick)
                .filter(v -> v != null)
                .min(Integer::compareTo)
                .orElse(null);
    }

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

    /** Hard cap on {@code GET /properties/popular-areas?size=}. */
    static final int POPULAR_AREAS_MAX_SIZE = 20;

    /** Floor for the same param — negative / zero get coerced to this. */
    static final int POPULAR_AREAS_MIN_SIZE = 1;

    /** Hard cap on {@code GET /properties/top-rated?size=}. */
    static final int TOP_RATED_MAX_SIZE = 20;

    /** Floor for the same param — negative / zero get coerced to this. */
    static final int TOP_RATED_MIN_SIZE = 1;

    /**
     * Suburbs with the most LISTED properties, capped at {@code size}.
     * Returns an editorial fallback ({@link LandingContent#EDITORIAL_AREAS}
     * with {@code listingCount = 0}) when no real listings exist yet so
     * the landing page never renders an empty "Popular: " line.
     */
    @Transactional(readOnly = true)
    public List<PopularAreaResponse> popularAreas(int size) {
        int safeSize = Math.min(POPULAR_AREAS_MAX_SIZE, Math.max(POPULAR_AREAS_MIN_SIZE, size));
        List<PopularAreaResponse> live = properties.findPopularSuburbs(
                PropertyStatus.LISTED, PageRequest.of(0, safeSize));
        if (!live.isEmpty()) {
            return live;
        }
        return LandingContent.EDITORIAL_AREAS.stream()
                .limit(safeSize)
                .map(name -> new PopularAreaResponse(name, 0L))
                .toList();
    }

    /**
     * Top-rated LISTED properties for the landing carousel. Sorts by
     * {@code avgRating DESC, ratingCount DESC, createdAt DESC} — until
     * reviews land, the catalogue is all-zeros and the third tiebreaker
     * makes this effectively "newest LISTED first", which is a sensible
     * fallback for an unrated catalogue (no fake data, no empty grid).
     */
    @Transactional(readOnly = true)
    public List<PropertySummary> topRated(int size) {
        int safeSize = Math.min(TOP_RATED_MAX_SIZE, Math.max(TOP_RATED_MIN_SIZE, size));
        List<Property> top = properties.findTopRated(
                PropertyStatus.LISTED, PageRequest.of(0, safeSize));
        return top.stream().map(PropertySummary::from).toList();
    }

    @Transactional(readOnly = true)
    public PropertyDetailResponse getById(UUID id) {
        Property p = findOrThrow(id);
        if (p.getStatus() != PropertyStatus.LISTED && !canEdit(p)) {
            throw new ResourceNotFoundException(ErrorMessages.PROPERTY_NOT_FOUND);
        }
        // Only the manager / landlord / admin sees the required-docs set;
        // anonymous /browse viewers don't need it.
        List<DocumentType> docs = canEdit(p)
                ? requiredDocs.findByProperty_Id(p.getId()).stream()
                        .map(PropertyRequiredDocument::getDocType)
                        .toList()
                : List.of();
        return PropertyDetailResponse.fromWithRequiredDocs(p, docs);
    }

    @Transactional
    public PropertyDetailResponse create(CreatePropertyRequest req) {
        UUID me = security.requireUserId();
        User user = users.findById(me)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));

        ListingMode mode = req.listingMode() == null ? ListingMode.LANDLORD_DIRECT : req.listingMode();
        validateMandatePair(mode, req.mandateFeePercent());

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
                .listingMode(mode)
                .mandateFeePercent(mode == ListingMode.AGENT_MANAGED ? req.mandateFeePercent() : null)
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
        if (req.listingMode() != null) {
            ListingMode mode = req.listingMode();
            // The fee tracks the mode it was assigned under — flipping back to
            // LANDLORD_DIRECT clears the previous fee.
            BigDecimal fee = req.mandateFeePercent() != null
                    ? req.mandateFeePercent() : p.getMandateFeePercent();
            validateMandatePair(mode, fee);
            p.setListingMode(mode);
            p.setMandateFeePercent(mode == ListingMode.AGENT_MANAGED ? fee : null);
        } else if (req.mandateFeePercent() != null) {
            validateMandatePair(p.getListingMode(), req.mandateFeePercent());
            p.setMandateFeePercent(req.mandateFeePercent());
        }

        log.info("property {} updated by user {}", p.getId(), security.requireUserId());
        return PropertyDetailResponse.from(p);
    }

    /**
     * Publish a draft listing. Idempotent — re-publishing a LISTED row is
     * a no-op. UNLISTED rows can be re-listed too (the wizard isn't the
     * only path; the dashboard might want to un-pause an old listing).
     */
    @Transactional
    public PropertyDetailResponse publish(UUID id) {
        Property p = findOrThrow(id);
        requireCanEdit(p);
        if (p.getStatus() == PropertyStatus.LISTED) {
            return PropertyDetailResponse.from(p);
        }
        // Guardrails — a listing with no rentable units is not a listing.
        long publishable = p.getUnits().stream()
                .filter(u -> u.getDeletedAt() == null)
                .count();
        if (publishable == 0) {
            throw new ConflictException(ErrorMessages.PROPERTY_NEEDS_UNIT_BEFORE_PUBLISH);
        }
        p.setStatus(PropertyStatus.LISTED);
        log.info("property {} published by user {}", p.getId(), security.requireUserId());
        return PropertyDetailResponse.from(p);
    }

    /**
     * Replace the required-documents set on a property. Idempotent.
     * Sends through the soft-delete path on rows that drop out so the
     * audit trail survives even when a landlord toggles requirements.
     */
    @Transactional
    public List<DocumentType> setRequiredDocuments(UUID propertyId, List<DocumentType> docTypes) {
        Property p = findOrThrow(propertyId);
        requireCanEdit(p);

        java.util.Set<DocumentType> desired = docTypes == null
                ? java.util.Set.of()
                : new java.util.LinkedHashSet<>(docTypes);
        java.util.List<PropertyRequiredDocument> existing =
                requiredDocs.findByProperty_Id(propertyId);
        java.util.Set<DocumentType> existingTypes = new java.util.HashSet<>();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (PropertyRequiredDocument row : existing) {
            existingTypes.add(row.getDocType());
            if (!desired.contains(row.getDocType())) {
                row.setDeletedAt(now);
            }
        }
        for (DocumentType type : desired) {
            if (!existingTypes.contains(type)) {
                requiredDocs.save(PropertyRequiredDocument.builder()
                        .property(p).docType(type).build());
            }
        }
        log.info("property {} required-docs set to {} by user {}",
                propertyId, desired, security.requireUserId());
        return new java.util.ArrayList<>(desired);
    }

    /**
     * Replace the amenity set on a property. Full-list replace,
     * idempotent. Rejects ids that aren't in the seeded amenities
     * catalogue rather than silently dropping them.
     */
    @Transactional
    public java.util.List<com.habitat.api.dto.property.AmenityResponse> setAmenities(
            UUID propertyId, java.util.List<UUID> amenityIds) {
        Property p = findOrThrow(propertyId);
        requireCanEdit(p);
        java.util.List<UUID> ids = amenityIds == null ? java.util.List.of() : amenityIds;
        java.util.List<Amenity> resolved = ids.isEmpty()
                ? java.util.List.of()
                : amenities.findAllByIdIn(ids);
        if (resolved.size() != new java.util.HashSet<>(ids).size()) {
            throw new BadRequestException(ErrorMessages.UNKNOWN_AMENITY);
        }
        p.getAmenities().clear();
        p.getAmenities().addAll(resolved);
        log.info("property {} amenities set to {} by user {}",
                propertyId, ids, security.requireUserId());
        return resolved.stream()
                .map(com.habitat.api.dto.property.AmenityResponse::from)
                .toList();
    }

    /** Caller's managed properties (any status), newest first. */
    @Transactional(readOnly = true)
    public PageResponse<PropertyDetailResponse> listManagedByMe(int page, int size) {
        UUID me = security.requireUserId();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        Page<Property> p = properties.findByManagerIdOrderByCreatedAtDesc(me, pageable);
        return PageResponse.from(p.map(PropertyDetailResponse::from));
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

    /**
     * Reject an inconsistent (mode, fee) pair. AGENT_MANAGED demands a
     * fee; LANDLORD_DIRECT must not carry one. The wizard's UI prevents
     * the bad pairing but a JSON caller could still send it.
     */
    private static void validateMandatePair(ListingMode mode, BigDecimal fee) {
        if (mode == ListingMode.AGENT_MANAGED && fee == null) {
            throw new ForbiddenException(ErrorMessages.MANDATE_FEE_REQUIRED);
        }
        if (mode == ListingMode.LANDLORD_DIRECT && fee != null && fee.signum() != 0) {
            throw new ForbiddenException(ErrorMessages.MANDATE_FEE_DISALLOWED);
        }
    }
}

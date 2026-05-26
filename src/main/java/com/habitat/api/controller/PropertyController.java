package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.constants.StorageConstants;
import com.habitat.api.dto.PageResponse;
import com.habitat.api.dto.property.CreatePropertyRequest;
import com.habitat.api.dto.property.CreateUnitRequest;
import com.habitat.api.dto.property.PopularAreaResponse;
import com.habitat.api.dto.property.PropertyDetailResponse;
import com.habitat.api.dto.property.PropertyImageResponse;
import com.habitat.api.dto.property.PropertySummary;
import com.habitat.api.dto.property.SetRequiredDocumentsRequest;
import com.habitat.api.dto.property.SetRequiredDocumentsResponse;
import com.habitat.api.dto.property.UnitResponse;
import com.habitat.api.dto.property.UpdatePropertyRequest;
import com.habitat.api.enums.UnitType;
import com.habitat.api.service.PropertyImageService;
import com.habitat.api.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.PROPERTIES)
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService properties;
    private final PropertyImageService propertyImages;

    @GetMapping
    public PageResponse<PropertySummary> search(
            @RequestParam(required = false) List<String> location,
            @RequestParam(name = "type", required = false) List<UnitType> unitTypes,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minBeds,
            @RequestParam(required = false) Integer minSqm,
            @RequestParam(required = false) PropertyService.SortKey sort,
            @RequestParam(required = false) PropertyService.SortDirection dir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return properties.search(location, unitTypes, minPrice, maxPrice, minBeds, minSqm, sort, dir, page, size);
    }

    @GetMapping("/popular-areas")
    public List<PopularAreaResponse> popularAreas(
            @RequestParam(defaultValue = "3") int size
    ) {
        return properties.popularAreas(size);
    }

    @GetMapping("/top-rated")
    public List<PropertySummary> topRated(
            @RequestParam(defaultValue = "4") int size
    ) {
        return properties.topRated(size);
    }

    /**
     * Caller's managed listings (any status). Powers the landlord
     * dashboard at /landlord-properties.
     */
    @GetMapping("/managed-by-me")
    public PageResponse<PropertyDetailResponse> listManagedByMe(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return properties.listManagedByMe(page, size);
    }

    @GetMapping("/{id}")
    public PropertyDetailResponse get(@PathVariable UUID id) {
        return properties.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyDetailResponse create(@Valid @RequestBody CreatePropertyRequest req) {
        return properties.create(req);
    }

    @PatchMapping("/{id}")
    public PropertyDetailResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePropertyRequest req
    ) {
        return properties.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        properties.delete(id);
    }

    @PostMapping("/{id}/units")
    @ResponseStatus(HttpStatus.CREATED)
    public UnitResponse addUnit(
            @PathVariable UUID id,
            @Valid @RequestBody CreateUnitRequest req
    ) {
        return properties.addUnit(id, req);
    }

    /**
     * Transition DRAFT (or UNLISTED) → LISTED. Idempotent on LISTED.
     * Fails with CONFLICT when no unit is attached yet.
     */
    @PatchMapping("/{id}/publish")
    public PropertyDetailResponse publish(@PathVariable UUID id) {
        return properties.publish(id);
    }

    /**
     * Replace the required-documents set on this property. Empty list
     * is allowed (zero required docs).
     */
    @PutMapping("/{id}/required-documents")
    public SetRequiredDocumentsResponse setRequiredDocs(
            @PathVariable UUID id,
            @Valid @RequestBody SetRequiredDocumentsRequest req
    ) {
        return new SetRequiredDocumentsResponse(
                properties.setRequiredDocuments(id, req.docTypes()));
    }

    /**
     * Upload a property photo. The {@code cover} param marks the
     * uploaded image as the new cover (unsets the previous cover).
     */
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyImageResponse uploadImage(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "cover", defaultValue = "false") boolean cover
    ) {
        return propertyImages.upload(id, file, cover, StorageConstants.FOLDER_PROPERTY_PHOTOS);
    }
}

package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.PageResponse;
import com.habitat.api.dto.property.CreatePropertyRequest;
import com.habitat.api.dto.property.CreateUnitRequest;
import com.habitat.api.dto.property.PropertyDetailResponse;
import com.habitat.api.dto.property.PropertySummary;
import com.habitat.api.dto.property.UnitResponse;
import com.habitat.api.dto.property.UpdatePropertyRequest;
import com.habitat.api.enums.UnitType;
import com.habitat.api.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.PROPERTIES)
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService properties;

    @GetMapping
    public PageResponse<PropertySummary> search(
            // Multi-select on the browse filter — Spring auto-splits a CSV
            // value so ?location=Sandton,Camps%20Bay binds as a 2-element list.
            // Each value is OR-matched against suburb/city/province.
            @RequestParam(required = false) List<String> location,
            @RequestParam(name = "type", required = false) List<UnitType> unitTypes,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minBeds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return properties.search(location, unitTypes, maxPrice, minBeds, page, size);
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
}

package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.property.UnitImageResponse;
import com.habitat.api.dto.property.UnitResponse;
import com.habitat.api.dto.property.UpdateUnitRequest;
import com.habitat.api.service.UnitImageService;
import com.habitat.api.service.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.UNITS)
@RequiredArgsConstructor
public class UnitController {

    private final UnitService units;
    private final UnitImageService unitImages;

    @PatchMapping("/{id}")
    public UnitResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUnitRequest req
    ) {
        return units.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        units.delete(id);
    }

    /** Upload a unit interior photo. */
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UnitImageResponse uploadImage(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "cover", defaultValue = "false") boolean cover
    ) {
        return unitImages.upload(id, file, cover);
    }
}

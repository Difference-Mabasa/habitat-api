package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.constants.StorageConstants;
import com.habitat.api.dto.property.UnitImageResponse;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.UnitImage;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.UnitImageRepository;
import com.habitat.api.repository.UnitRepository;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.storage.StorageService;
import com.habitat.api.storage.StoredFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** Mirror of {@link PropertyImageService} for {@link UnitImage}. */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnitImageService {

    private final UnitRepository units;
    private final UnitImageRepository unitImages;
    private final StorageService storage;
    private final PropertyService propertyService;
    private final SecurityUtils security;

    @Transactional
    public UnitImageResponse upload(UUID unitId, MultipartFile file, boolean cover) {
        Unit u = units.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.UNIT_NOT_FOUND));
        propertyService.requireCanEdit(u.getProperty());

        StoredFile stored = storage.store(
                StorageConstants.FOLDER_UNIT_PHOTOS,
                file,
                StorageConstants.ALLOWED_IMAGE_TYPES,
                StorageConstants.MAX_IMAGE_BYTES);

        if (cover) {
            for (UnitImage existing : u.getImages()) {
                if (Boolean.TRUE.equals(existing.getIsCover())) {
                    existing.setIsCover(false);
                }
            }
        }
        int nextSortOrder = u.getImages().stream()
                .mapToInt(img -> img.getSortOrder() == null ? 0 : img.getSortOrder())
                .max()
                .orElse(-1) + 1;

        UnitImage row = UnitImage.builder()
                .unit(u)
                .url(stored.storedPath())
                .isCover(cover)
                .sortOrder(nextSortOrder)
                .build();
        UnitImage saved = unitImages.save(row);
        log.info("unit {} image {} uploaded by user {}",
                unitId, saved.getId(), security.requireUserId());
        return UnitImageResponse.from(saved);
    }

    @Transactional
    public void delete(UUID imageId) {
        UnitImage img = unitImages.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.INVALID_FILE_PATH));
        if (!propertyService.canEdit(img.getUnit().getProperty())) {
            throw new ForbiddenException(ErrorMessages.FORBIDDEN);
        }
        if (img.getUrl() != null && !img.getUrl().startsWith("http")) {
            storage.delete(img.getUrl());
        }
        img.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        log.info("unit image {} deleted by user {}", imageId, security.requireUserId());
    }
}

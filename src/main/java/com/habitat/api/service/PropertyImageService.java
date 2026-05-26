package com.habitat.api.service;

import com.habitat.api.constants.StorageConstants;
import com.habitat.api.dto.property.PropertyImageResponse;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.PropertyImage;
import com.habitat.api.repository.PropertyImageRepository;
import com.habitat.api.repository.PropertyRepository;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.storage.StorageService;
import com.habitat.api.storage.StoredFile;
import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Property photo upload + delete. Uses StorageService for the bytes
 * (Tika magic-byte validation + size cap from
 * {@link StorageConstants#MAX_IMAGE_BYTES}); persists a
 * {@link PropertyImage} row whose {@code url} field carries the
 * storage-relative path. The response DTO routes that path through
 * the public {@code /api/v1/files/images/} endpoint.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyImageService {

    private final PropertyRepository properties;
    private final PropertyImageRepository propertyImages;
    private final StorageService storage;
    private final PropertyService propertyService;
    private final SecurityUtils security;

    @Transactional
    public PropertyImageResponse upload(UUID propertyId, MultipartFile file, boolean cover, String folder) {
        Property p = properties.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.PROPERTY_NOT_FOUND));
        propertyService.requireCanEdit(p);

        StoredFile stored = storage.store(
                folder,
                file,
                StorageConstants.ALLOWED_IMAGE_TYPES,
                StorageConstants.MAX_IMAGE_BYTES);

        if (cover) {
            // Unset the previous cover so only the new upload is flagged.
            for (PropertyImage existing : p.getImages()) {
                if (Boolean.TRUE.equals(existing.getIsCover())) {
                    existing.setIsCover(false);
                }
            }
        }

        // Sort orders climb monotonically — the cover image stays at 0 if it
        // existed; otherwise the first uploaded image takes the slot.
        int nextSortOrder = p.getImages().stream()
                .mapToInt(img -> img.getSortOrder() == null ? 0 : img.getSortOrder())
                .max()
                .orElse(-1) + 1;

        PropertyImage row = PropertyImage.builder()
                .property(p)
                .url(stored.storedPath())
                .isCover(cover)
                .sortOrder(nextSortOrder)
                .build();
        PropertyImage saved = propertyImages.save(row);
        log.info("property {} image {} uploaded by user {}",
                propertyId, saved.getId(), security.requireUserId());
        return PropertyImageResponse.from(saved);
    }

    @Transactional
    public void delete(UUID imageId) {
        PropertyImage img = propertyImages.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.INVALID_FILE_PATH));
        if (!propertyService.canEdit(img.getProperty())) {
            throw new ForbiddenException(ErrorMessages.FORBIDDEN);
        }
        // Storage delete is idempotent; soft-delete the row to keep audit.
        if (img.getUrl() != null && !img.getUrl().startsWith("http")) {
            storage.delete(img.getUrl());
        }
        img.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        log.info("property image {} deleted by user {}", imageId, security.requireUserId());
    }
}

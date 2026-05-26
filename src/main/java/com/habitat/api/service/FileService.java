package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.entity.Application;
import com.habitat.api.entity.ApplicationDocument;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.ApplicationDocumentRepository;
import com.habitat.api.repository.ApplicationRepository;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.storage.StorageService;
import com.habitat.api.storage.StoredResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Owns the lookup + ownership check + storage handoff for every
 * authenticated download. Lives in the service layer (not the
 * controller) so the transaction wraps the entity-graph walk —
 * application → unit → property → manager — without depending on
 * open-session-in-view.
 */
@Service
@RequiredArgsConstructor
public class FileService {

    private final ApplicationRepository applications;
    private final ApplicationDocumentRepository appDocs;
    private final StorageService storage;
    private final SecurityUtils security;

    /**
     * Look up the document, verify the caller is the application owner
     * or the property's manager, and return a streaming handle plus the
     * file metadata needed for response headers.
     */
    @Transactional(readOnly = true)
    public DownloadHandle openApplicationDocument(UUID applicationId, UUID docId) {
        UUID me = security.requireUserId();
        Application application = applications.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.APPLICATION_NOT_FOUND));

        UUID tenantId = application.getTenant().getId();
        UUID managerId = application.getUnit().getProperty().getManager() == null
                ? null
                : application.getUnit().getProperty().getManager().getId();
        if (!me.equals(tenantId) && !me.equals(managerId)) {
            throw new ForbiddenException(ErrorMessages.FORBIDDEN);
        }

        ApplicationDocument doc = appDocs.findById(docId)
                .filter(d -> d.getApplication().getId().equals(applicationId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.INVALID_FILE_PATH));

        StoredResource resource = storage.open(doc.getFileUrl());
        String safeName = doc.getFileName() == null ? "document" : doc.getFileName();
        return new DownloadHandle(resource, safeName);
    }

    public record DownloadHandle(StoredResource resource, String fileName) {}
}

package com.habitat.api.repository;

import com.habitat.api.entity.ApplicationDocument;
import com.habitat.api.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, UUID> {

    List<ApplicationDocument> findByApplication_Id(UUID applicationId);

    /** Existing record for a (application, docType) pair — drives the upsert path on re-upload. */
    Optional<ApplicationDocument> findByApplication_IdAndDocType(UUID applicationId, DocumentType docType);
}

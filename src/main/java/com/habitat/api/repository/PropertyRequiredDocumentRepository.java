package com.habitat.api.repository;

import com.habitat.api.entity.PropertyRequiredDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PropertyRequiredDocumentRepository extends JpaRepository<PropertyRequiredDocument, UUID> {

    /** Required document types for a property — drives the apply-flow auto-transition. */
    List<PropertyRequiredDocument> findByProperty_Id(UUID propertyId);
}

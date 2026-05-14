package com.habitat.api.repository;

import com.habitat.api.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    /** Invoices belonging to a tenant. Newest first. */
    List<Invoice> findByTenant_IdOrderByCreatedAtDesc(UUID tenantId);

    /** The invoice generated for a specific application — at most one per application. */
    Optional<Invoice> findByApplication_Id(UUID applicationId);
}

package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.application.ApplicationDocumentResponse;
import com.habitat.api.dto.application.ApplicationResponse;
import com.habitat.api.dto.application.CreateApplicationRequest;
import com.habitat.api.dto.application.RequiredDocumentsResponse;
import com.habitat.api.dto.application.ReviewApplicationRequest;
import com.habitat.api.enums.DocumentType;
import com.habitat.api.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
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

import java.util.List;
import java.util.UUID;

/**
 * Applications endpoint. Sits on {@code /applications}. Mixes tenant
 * actions (create, upload docs, my list) with landlord actions (inbound
 * list, review). Authentication is required everywhere — role-level
 * authorisation is enforced inside the service.
 */
@RestController
@RequestMapping(ApiRoutes.APPLICATIONS)
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applications;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(@Valid @RequestBody CreateApplicationRequest req) {
        return applications.create(req);
    }

    /** Caller's own applications, newest first — powers /my-apps. */
    @GetMapping("/me")
    public List<ApplicationResponse> listMine() {
        return applications.listForTenant();
    }

    /** Applications received on properties the caller manages — powers the landlord inbox. */
    @GetMapping("/inbound")
    public List<ApplicationResponse> listInbound() {
        return applications.listInboundForLandlord();
    }

    /** Single application — visible to the tenant who owns it OR the property manager. */
    @GetMapping("/{id}")
    public ApplicationResponse get(@PathVariable UUID id) {
        return applications.getById(id);
    }

    /** Landlord verdict: APPROVE / REJECT / ON_HOLD with optional note. */
    @PatchMapping("/{id}/review")
    public ApplicationResponse review(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewApplicationRequest req
    ) {
        return applications.review(id, req);
    }

    /** Documents the landlord requires + what's already uploaded. */
    @GetMapping("/{id}/documents")
    public RequiredDocumentsResponse listDocuments(@PathVariable UUID id) {
        return applications.listRequiredDocuments(id);
    }

    /**
     * Multipart upload — the tenant POSTs the binary file plus the
     * doc-type discriminator. Tika magic-byte detection + the
     * StorageService size cap run before any DB row is written, so a
     * rejected upload never leaves a phantom document record behind.
     */
    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDocumentResponse uploadDocument(
            @PathVariable UUID id,
            @RequestParam("docType") DocumentType docType,
            @RequestPart("file") MultipartFile file
    ) {
        return applications.uploadDocument(id, docType, file);
    }
}

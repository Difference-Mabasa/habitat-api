package com.habitat.api.service;

import com.habitat.api.dto.application.ApplicationResponse;
import com.habitat.api.dto.application.CreateApplicationRequest;
import com.habitat.api.dto.application.ReviewApplicationRequest;
import com.habitat.api.dto.application.UploadDocumentRequest;
import com.habitat.api.entity.Application;
import com.habitat.api.entity.ApplicationDocument;
import com.habitat.api.entity.PropertyRequiredDocument;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.User;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.enums.DocumentType;
import com.habitat.api.enums.EmploymentStatus;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.entity.Property;
import com.habitat.api.repository.ApplicationDocumentRepository;
import com.habitat.api.repository.ApplicationRepository;
import com.habitat.api.repository.PropertyRequiredDocumentRepository;
import com.habitat.api.repository.UnitRepository;
import com.habitat.api.repository.UserRepository;
import com.habitat.api.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock ApplicationRepository applications;
    @Mock UnitRepository units;
    @Mock UserRepository users;
    @Mock PropertyRequiredDocumentRepository requiredDocs;
    @Mock ApplicationDocumentRepository appDocs;
    @Mock org.springframework.context.ApplicationEventPublisher events;
    @Mock SecurityUtils security;
    @InjectMocks ApplicationService service;

    private static final UUID TENANT_ID   = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID UNIT_ID     = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROPERTY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void create_persists_an_application_in_SUBMITTED() {
        Unit unit = unitWith(UnitStatus.AVAILABLE);
        User tenant = userWithId(TENANT_ID);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(units.findById(UNIT_ID)).thenReturn(Optional.of(unit));
        when(applications.existsByUnit_IdAndTenant_Id(UNIT_ID, TENANT_ID)).thenReturn(false);
        when(users.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(applications.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse out = service.create(new CreateApplicationRequest(
                UNIT_ID, "Hi!", LocalDate.parse("2026-08-01"), EmploymentStatus.EMPLOYED));

        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(applications).save(captor.capture());
        Application saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(saved.getMessage()).isEqualTo("Hi!");
        assertThat(saved.getEmploymentStatus()).isEqualTo(EmploymentStatus.EMPLOYED);
        assertThat(saved.getTenant()).isSameAs(tenant);
        assertThat(saved.getUnit()).isSameAs(unit);
        assertThat(out.status()).isEqualTo(ApplicationStatus.SUBMITTED);
        verify(events).publishEvent(any(com.habitat.api.event.ApplicationSubmittedEvent.class));
    }

    @Test
    void create_throws_when_unit_does_not_exist() {
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(units.findById(UNIT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateApplicationRequest(
                UNIT_ID, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(applications, never()).save(any());
    }

    @Test
    void create_throws_when_unit_not_available() {
        Unit unit = unitWith(UnitStatus.OCCUPIED);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(units.findById(UNIT_ID)).thenReturn(Optional.of(unit));

        assertThatThrownBy(() -> service.create(new CreateApplicationRequest(
                UNIT_ID, null, null, null)))
                .isInstanceOf(ConflictException.class);
        verify(applications, never()).save(any());
    }

    @Test
    void create_throws_when_tenant_already_applied() {
        Unit unit = unitWith(UnitStatus.AVAILABLE);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(units.findById(UNIT_ID)).thenReturn(Optional.of(unit));
        when(applications.existsByUnit_IdAndTenant_Id(UNIT_ID, TENANT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateApplicationRequest(
                UNIT_ID, null, null, null)))
                .isInstanceOf(ConflictException.class);
        verify(applications, never()).save(any());
    }

    @Test
    void create_accepts_a_minimal_payload_with_only_unitId() {
        Unit unit = unitWith(UnitStatus.AVAILABLE);
        User tenant = userWithId(TENANT_ID);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(units.findById(UNIT_ID)).thenReturn(Optional.of(unit));
        when(applications.existsByUnit_IdAndTenant_Id(UNIT_ID, TENANT_ID)).thenReturn(false);
        when(users.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(applications.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse out = service.create(new CreateApplicationRequest(UNIT_ID, null, null, null));

        assertThat(out.status()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(out.message()).isNull();
        assertThat(out.moveInDate()).isNull();
        assertThat(out.employmentStatus()).isNull();
    }

    @Test
    void create_auto_transitions_to_AWAITING_DOCUMENTS_when_property_requires_docs() {
        Unit unit = unitWith(UnitStatus.AVAILABLE);
        User tenant = userWithId(TENANT_ID);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(units.findById(UNIT_ID)).thenReturn(Optional.of(unit));
        when(applications.existsByUnit_IdAndTenant_Id(UNIT_ID, TENANT_ID)).thenReturn(false);
        when(users.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(requiredDocs.findByProperty_Id(PROPERTY_ID))
                .thenReturn(java.util.List.of(
                        PropertyRequiredDocument.builder().docType(DocumentType.SA_ID).build()));
        when(applications.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        var out = service.create(new com.habitat.api.dto.application.CreateApplicationRequest(
                UNIT_ID, null, null, null));

        assertThat(out.status()).isEqualTo(ApplicationStatus.AWAITING_DOCUMENTS);
    }

    @Test
    void uploadDocument_records_the_file_under_the_application() {
        Application app = applicationWith(ApplicationStatus.AWAITING_DOCUMENTS);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(applications.findById(app.getId())).thenReturn(Optional.of(app));
        when(appDocs.findByApplication_IdAndDocType(app.getId(), DocumentType.SA_ID))
                .thenReturn(Optional.empty());
        when(appDocs.save(any(ApplicationDocument.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requiredDocs.findByProperty_Id(PROPERTY_ID))
                .thenReturn(java.util.List.of(
                        PropertyRequiredDocument.builder().docType(DocumentType.SA_ID).build()));
        when(appDocs.findByApplication_Id(app.getId()))
                .thenReturn(java.util.List.of(
                        ApplicationDocument.builder().docType(DocumentType.SA_ID).build()));

        var out = service.uploadDocument(app.getId(),
                new UploadDocumentRequest(DocumentType.SA_ID, "id-front.jpg", "/uploads/stub/id-front.jpg"));

        assertThat(out.docType()).isEqualTo(DocumentType.SA_ID);
        assertThat(out.fileName()).isEqualTo("id-front.jpg");
        // All required types now uploaded — status flips.
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.DOCUMENTS_SUBMITTED);
    }

    @Test
    void uploadDocument_keeps_status_when_more_required_docs_remain() {
        Application app = applicationWith(ApplicationStatus.AWAITING_DOCUMENTS);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(applications.findById(app.getId())).thenReturn(Optional.of(app));
        when(appDocs.findByApplication_IdAndDocType(any(), any())).thenReturn(Optional.empty());
        when(appDocs.save(any(ApplicationDocument.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requiredDocs.findByProperty_Id(PROPERTY_ID))
                .thenReturn(java.util.List.of(
                        PropertyRequiredDocument.builder().docType(DocumentType.SA_ID).build(),
                        PropertyRequiredDocument.builder().docType(DocumentType.PAYSLIPS_3_MONTHS).build()));
        when(appDocs.findByApplication_Id(app.getId()))
                .thenReturn(java.util.List.of(
                        ApplicationDocument.builder().docType(DocumentType.SA_ID).build()));

        service.uploadDocument(app.getId(),
                new UploadDocumentRequest(DocumentType.SA_ID, "id.jpg", "/uploads/stub/id.jpg"));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.AWAITING_DOCUMENTS);
    }

    // ── listForTenant / listInboundForLandlord / getById ──────────────

    @Test
    void listForTenant_returns_the_callers_applications_newest_first() {
        Application a = applicationWith(ApplicationStatus.SUBMITTED);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(applications.findByTenant_IdOrderByCreatedAtDesc(TENANT_ID))
                .thenReturn(java.util.List.of(a));

        var out = service.listForTenant();

        assertThat(out).hasSize(1);
        assertThat(out.get(0).id()).isEqualTo(a.getId());
        assertThat(out.get(0).status()).isEqualTo(ApplicationStatus.SUBMITTED);
    }

    @Test
    void listInbound_returns_applications_received_against_managed_properties() {
        UUID managerId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        Application a = applicationWithManager(managerId);
        when(security.requireUserId()).thenReturn(managerId);
        when(applications.findByUnit_Property_Manager_IdOrderByCreatedAtDesc(managerId))
                .thenReturn(java.util.List.of(a));

        var out = service.listInboundForLandlord();

        assertThat(out).hasSize(1);
        assertThat(out.get(0).tenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void getById_visible_to_the_owning_tenant() {
        Application a = applicationWith(ApplicationStatus.SUBMITTED);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(applications.findById(a.getId())).thenReturn(Optional.of(a));

        var out = service.getById(a.getId());

        assertThat(out.id()).isEqualTo(a.getId());
    }

    @Test
    void getById_visible_to_the_property_manager() {
        UUID managerId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        Application a = applicationWithManager(managerId);
        when(security.requireUserId()).thenReturn(managerId);
        when(applications.findById(a.getId())).thenReturn(Optional.of(a));

        var out = service.getById(a.getId());

        assertThat(out.id()).isEqualTo(a.getId());
    }

    @Test
    void getById_forbidden_for_strangers() {
        UUID stranger = UUID.fromString("77777777-7777-7777-7777-777777777777");
        Application a = applicationWith(ApplicationStatus.SUBMITTED);
        when(security.requireUserId()).thenReturn(stranger);
        when(applications.findById(a.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.getById(a.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getById_throws_when_missing() {
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(applications.findById(APP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(APP_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── review ────────────────────────────────────────────────────────

    @Test
    void review_approve_lands_at_APPROVED_and_publishes_event() {
        UUID managerId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        Application a = applicationWithManager(managerId);
        a.setStatus(ApplicationStatus.DOCUMENTS_SUBMITTED);
        when(security.requireUserId()).thenReturn(managerId);
        when(applications.findById(a.getId())).thenReturn(Optional.of(a));
        // ARCH-04: ApplicationService snapshots the reviewer's name.
        when(users.findById(managerId)).thenReturn(Optional.of(
                a.getUnit().getProperty().getManager()));

        var out = service.review(a.getId(),
                new ReviewApplicationRequest(ReviewApplicationRequest.Action.APPROVE, "Looks great"));

        // ARCH-03: ApplicationService no longer issues the invoice
        // synchronously — it publishes an event the listener handles.
        // So at the end of review() the status is APPROVED, not
        // INVOICE_SENT; INVOICE_SENT lands AFTER_COMMIT in the listener.
        assertThat(a.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(a.getDecisionNote()).isEqualTo("Looks great");
        assertThat(a.getDecidedBy()).isEqualTo(managerId);
        assertThat(a.getDecidedByName()).isEqualTo("Test Tester");
        assertThat(a.getDecidedAt()).isNotNull();
        assertThat(out.status()).isEqualTo(ApplicationStatus.APPROVED);
        verify(events).publishEvent(
                new com.habitat.api.event.ApplicationApprovedEvent(a.getId()));
    }

    @Test
    void review_reject_transitions_to_REJECTED() {
        UUID managerId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        Application a = applicationWithManager(managerId);
        when(security.requireUserId()).thenReturn(managerId);
        when(applications.findById(a.getId())).thenReturn(Optional.of(a));

        service.review(a.getId(),
                new ReviewApplicationRequest(ReviewApplicationRequest.Action.REJECT, "Income too low"));

        assertThat(a.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void review_on_hold_transitions_to_ON_HOLD() {
        UUID managerId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        Application a = applicationWithManager(managerId);
        when(security.requireUserId()).thenReturn(managerId);
        when(applications.findById(a.getId())).thenReturn(Optional.of(a));

        service.review(a.getId(),
                new ReviewApplicationRequest(ReviewApplicationRequest.Action.ON_HOLD, "Need references"));

        assertThat(a.getStatus()).isEqualTo(ApplicationStatus.ON_HOLD);
    }

    @Test
    void review_forbidden_when_caller_is_not_the_manager() {
        UUID managerId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID other     = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        Application a = applicationWithManager(managerId);
        when(security.requireUserId()).thenReturn(other);
        when(applications.findById(a.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.review(a.getId(),
                new ReviewApplicationRequest(ReviewApplicationRequest.Action.APPROVE, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void review_rejected_when_application_is_in_a_terminal_state() {
        UUID managerId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        Application a = applicationWithManager(managerId);
        a.setStatus(ApplicationStatus.COMPLETED);
        when(security.requireUserId()).thenReturn(managerId);
        when(applications.findById(a.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.review(a.getId(),
                new ReviewApplicationRequest(ReviewApplicationRequest.Action.APPROVE, null)))
                .isInstanceOf(ConflictException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Unit unitWith(UnitStatus status) {
        Property property = Property.builder().build();
        setId(property, PROPERTY_ID);
        Unit u = Unit.builder().status(status).property(property).build();
        setId(u, UNIT_ID);
        return u;
    }

    private Unit unitWithManager(UUID managerId) {
        User manager = userWithId(managerId);
        Property property = Property.builder().manager(manager).build();
        setId(property, PROPERTY_ID);
        Unit u = Unit.builder().status(UnitStatus.AVAILABLE).property(property).build();
        setId(u, UNIT_ID);
        return u;
    }

    private Application applicationWithManager(UUID managerId) {
        Unit unit = unitWithManager(managerId);
        User tenant = userWithId(TENANT_ID);
        Application app = Application.builder()
                .unit(unit)
                .tenant(tenant)
                .status(ApplicationStatus.SUBMITTED)
                .build();
        setId(app, APP_ID);
        return app;
    }

    private static final UUID APP_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private Application applicationWith(ApplicationStatus status) {
        Unit unit = unitWith(UnitStatus.AVAILABLE);
        User tenant = userWithId(TENANT_ID);
        Application app = Application.builder()
                .unit(unit)
                .tenant(tenant)
                .status(status)
                .build();
        setId(app, APP_ID);
        return app;
    }

    private User userWithId(UUID id) {
        User u = User.builder().firstName("Test").surname("Tester").email("t@example.co.za").build();
        setId(u, id);
        return u;
    }

    private static void setId(Object entity, UUID id) {
        try {
            Field f = entity.getClass().getSuperclass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package com.habitat.api.service;

import com.habitat.api.dto.invoice.PayInvoiceRequest;
import com.habitat.api.entity.Application;
import com.habitat.api.entity.Invoice;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.User;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.enums.InvoiceStatus;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.InvoiceRepository;
import com.habitat.api.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock InvoiceRepository invoices;
    @Mock SecurityUtils security;
    @InjectMocks InvoiceService service;

    private static final UUID TENANT_ID  = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID UNIT_ID    = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROP_ID    = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID APP_ID     = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID INVOICE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Test
    void issueForApprovedApplication_creates_invoice_with_deposit_plus_first_month() {
        Application app = applicationWith(ApplicationStatus.APPROVED, new BigDecimal("8500.00"));
        when(invoices.findByApplication_Id(app.getId())).thenReturn(Optional.empty());
        when(invoices.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice out = service.issueForApprovedApplication(app);

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoices).save(captor.capture());
        Invoice saved = captor.getValue();
        assertThat(saved.getDepositAmount()).isEqualByComparingTo("8500.00");
        assertThat(saved.getFirstMonthRent()).isEqualByComparingTo("8500.00");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("17000.00");
        assertThat(saved.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(saved.getInvoiceRef()).startsWith("HB-INV-");
        assertThat(saved.getExpiresAt()).isAfter(saved.getIssuedAt());
        assertThat(out).isSameAs(saved);
    }

    @Test
    void issueForApprovedApplication_is_idempotent() {
        Application app = applicationWith(ApplicationStatus.APPROVED, new BigDecimal("5000"));
        Invoice existing = invoiceWith(InvoiceStatus.PENDING, app);
        when(invoices.findByApplication_Id(app.getId())).thenReturn(Optional.of(existing));

        Invoice out = service.issueForApprovedApplication(app);

        assertThat(out).isSameAs(existing);
        verify(invoices, never()).save(any(Invoice.class));
    }

    @Test
    void listForTenant_returns_invoices_for_caller() {
        Application app = applicationWith(ApplicationStatus.INVOICE_SENT, new BigDecimal("5000"));
        Invoice i = invoiceWith(InvoiceStatus.PENDING, app);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(invoices.findByTenant_IdOrderByCreatedAtDesc(TENANT_ID))
                .thenReturn(java.util.List.of(i));

        var out = service.listForTenant();

        assertThat(out).hasSize(1);
        assertThat(out.get(0).status()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(out.get(0).tenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void getById_returns_for_owning_tenant() {
        Application app = applicationWith(ApplicationStatus.INVOICE_SENT, new BigDecimal("5000"));
        Invoice i = invoiceWith(InvoiceStatus.PENDING, app);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));

        var out = service.getById(i.getId());

        assertThat(out.id()).isEqualTo(i.getId());
    }

    @Test
    void getById_forbidden_for_other_users() {
        Application app = applicationWith(ApplicationStatus.INVOICE_SENT, new BigDecimal("5000"));
        Invoice i = invoiceWith(InvoiceStatus.PENDING, app);
        UUID other = UUID.fromString("99999999-9999-9999-9999-999999999999");
        when(security.requireUserId()).thenReturn(other);
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));

        assertThatThrownBy(() -> service.getById(i.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getById_throws_when_missing() {
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(invoices.findById(INVOICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(INVOICE_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void pay_flips_invoice_to_PAID_and_advances_application_to_DEPOSIT_PAID() {
        Application app = applicationWith(ApplicationStatus.INVOICE_SENT, new BigDecimal("5000"));
        Invoice i = invoiceWith(InvoiceStatus.PENDING, app);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));

        var out = service.pay(i.getId(), new PayInvoiceRequest("OZOW-REF-XYZ"));

        assertThat(i.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(i.getPaidAt()).isNotNull();
        assertThat(i.getPaymentReference()).isEqualTo("OZOW-REF-XYZ");
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.DEPOSIT_PAID);
        assertThat(out.status()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void pay_is_idempotent_when_already_paid() {
        Application app = applicationWith(ApplicationStatus.DEPOSIT_PAID, new BigDecimal("5000"));
        Invoice i = invoiceWith(InvoiceStatus.PAID, app);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));

        var out = service.pay(i.getId(), null);

        assertThat(out.status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.DEPOSIT_PAID);
    }

    @Test
    void pay_rejects_voided_invoice() {
        Application app = applicationWith(ApplicationStatus.WITHDRAWN, new BigDecimal("5000"));
        Invoice i = invoiceWith(InvoiceStatus.VOIDED, app);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));

        assertThatThrownBy(() -> service.pay(i.getId(), null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void pay_forbidden_for_other_users() {
        Application app = applicationWith(ApplicationStatus.INVOICE_SENT, new BigDecimal("5000"));
        Invoice i = invoiceWith(InvoiceStatus.PENDING, app);
        UUID other = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(security.requireUserId()).thenReturn(other);
        when(invoices.findById(i.getId())).thenReturn(Optional.of(i));

        assertThatThrownBy(() -> service.pay(i.getId(), null))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Application applicationWith(ApplicationStatus status, BigDecimal price) {
        Property property = Property.builder().build();
        setId(property, PROP_ID);
        Unit unit = Unit.builder().status(UnitStatus.AVAILABLE).property(property).price(price).build();
        setId(unit, UNIT_ID);
        User tenant = User.builder()
                .firstName("T").surname("Test").email("t@example.co.za").build();
        setId(tenant, TENANT_ID);
        Application app = Application.builder()
                .unit(unit).tenant(tenant).status(status).build();
        setId(app, APP_ID);
        return app;
    }

    private Invoice invoiceWith(InvoiceStatus status, Application app) {
        Invoice i = Invoice.builder()
                .application(app)
                .tenant(app.getTenant())
                .depositAmount(new BigDecimal("5000"))
                .firstMonthRent(new BigDecimal("5000"))
                .totalAmount(new BigDecimal("10000"))
                .status(status)
                .invoiceRef("HB-INV-TESTREF")
                .build();
        setId(i, INVOICE_ID);
        return i;
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

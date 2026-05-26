package com.habitat.api.service;

import com.habitat.api.entity.Application;
import com.habitat.api.entity.Lease;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.User;
import com.habitat.api.enums.LeaseStatus;
import com.habitat.api.enums.LeaseTemplate;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rendering logic is mostly String.format — but we want at least
 * one assertion that openhtmltopdf is wired correctly and produces a
 * real PDF byte sequence (so a missing dep or a malformed template
 * shows up here, not at sign-time in production).
 */
class LeasePdfServiceTest {

    private final LeasePdfService service = new LeasePdfService();

    @Test
    void render_produces_pdf_bytes_starting_with_the_pdf_magic_header() {
        byte[] pdf = service.render(fullLease());

        assertThat(pdf).isNotEmpty();
        // PDF magic bytes: %PDF
        assertThat(pdf[0]).isEqualTo((byte) '%');
        assertThat(pdf[1]).isEqualTo((byte) 'P');
        assertThat(pdf[2]).isEqualTo((byte) 'D');
        assertThat(pdf[3]).isEqualTo((byte) 'F');
    }

    @Test
    void render_handles_a_lease_with_only_one_signature_present() {
        // PDF generation happens BEFORE the second sign timestamp lands,
        // so the rendered file must still be valid when one party hasn't
        // signed yet.
        Lease half = fullLease();
        half.setLandlordSignedAt(null);

        byte[] pdf = service.render(half);

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void render_handles_missing_snapshots() {
        Lease bare = fullLease();
        bare.setTenantNameSnapshot(null);
        bare.setLandlordNameSnapshot(null);
        bare.setUnitTitleSnapshot(null);
        bare.setPropertyTitleSnapshot(null);
        bare.setPropertyAddressSnapshot(null);
        bare.setStartDate(null);
        bare.setMonthlyRent(null);
        bare.setDeposit(null);

        byte[] pdf = service.render(bare);

        assertThat(pdf).isNotEmpty();
    }

    private static Lease fullLease() {
        User tenant = withId(User.builder().firstName("Sipho").surname("Mahlangu")
                .email("sipho@example.co.za").build(), UUID.randomUUID());
        User landlord = withId(User.builder().firstName("Thandi").surname("Vilakazi")
                .email("thandi@example.co.za").build(), UUID.randomUUID());
        Property property = withId(Property.builder().title("Olive Court")
                .manager(landlord).build(), UUID.randomUUID());
        Unit unit = withId(Unit.builder().title("Unit 1B").property(property)
                .price(new BigDecimal("8500")).build(), UUID.randomUUID());
        Application app = withId(Application.builder().tenant(tenant).unit(unit).build(),
                UUID.randomUUID());

        OffsetDateTime now = OffsetDateTime.parse("2026-05-20T10:00:00Z");
        Lease lease = Lease.builder()
                .application(app)
                .tenant(tenant)
                .landlord(landlord)
                .unit(unit)
                .property(property)
                .template(LeaseTemplate.RHA_STANDARD)
                .monthlyRent(new BigDecimal("8500"))
                .deposit(new BigDecimal("8500"))
                .termMonths(12)
                .startDate(LocalDate.parse("2026-08-01"))
                .status(LeaseStatus.PENDING_SIGNATURES)
                .leaseRef("HB-LSE-TEST123")
                .tenantNameSnapshot("Sipho Mahlangu")
                .landlordNameSnapshot("Thandi Vilakazi")
                .unitTitleSnapshot("Unit 1B")
                .propertyTitleSnapshot("Olive Court")
                .propertyAddressSnapshot("12 Olive Rd, Yeoville, Joburg")
                .tenantSignedAt(now)
                .landlordSignedAt(now.plusMinutes(5))
                .build();
        return withId(lease, UUID.randomUUID());
    }

    private static <T> T withId(T entity, UUID id) {
        try {
            Field f = entity.getClass().getSuperclass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return entity;
    }
}

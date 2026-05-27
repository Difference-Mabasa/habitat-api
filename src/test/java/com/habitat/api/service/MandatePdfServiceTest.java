package com.habitat.api.service;

import com.habitat.api.entity.Mandate;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.User;
import com.habitat.api.enums.MandateStatus;
import com.habitat.api.enums.MandateType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mirrors {@link LeasePdfServiceTest}: prove openhtmltopdf wiring
 * produces a real PDF, and cover the sig-stamp branches so the offline
 * / online / awaiting paths each render the matching label.
 */
class MandatePdfServiceTest {

    private final PdfTemplateService templates = new PdfTemplateService();
    private final MandatePdfService service = new MandatePdfService(templates);

    @Test
    void render_produces_pdf_bytes_starting_with_the_pdf_magic_header() {
        byte[] pdf = service.render(offlineMandate());

        assertThat(pdf).isNotEmpty();
        assertThat(pdf[0]).isEqualTo((byte) '%');
        assertThat(pdf[1]).isEqualTo((byte) 'P');
        assertThat(pdf[2]).isEqualTo((byte) 'D');
        assertThat(pdf[3]).isEqualTo((byte) 'F');
    }

    @Test
    void renderHtml_offline_unsigned_shows_awaiting_sig_and_attestation() {
        String html = renderHtml(offlineMandate());
        assertThat(html).contains("Landlord · awaiting signature");
        assertThat(html).contains("Agent · awaiting attestation");
    }

    @Test
    void renderHtml_offline_signed_shows_signed_sast_stamp() {
        Mandate m = offlineMandate();
        m.setSignedDocumentPath("storage/signed.pdf");
        setField(m, "updatedAt", OffsetDateTime.parse("2026-05-21T10:00:00Z"));

        String html = renderHtml(m);
        // 10:00 UTC → 12:00 SAST (UTC+2, no DST in SA).
        assertThat(html).contains("Landlord · signed 21 May 2026 12:00 SAST");
    }

    @Test
    void renderHtml_online_active_shows_approved_sast_stamp() {
        Mandate m = offlineMandate();
        m.setOfflineLandlordName(null);
        m.setOfflineLandlordEmail(null);
        m.setLandlordUser(landlordUser());
        m.setStatus(MandateStatus.ACTIVE);
        setField(m, "updatedAt", OffsetDateTime.parse("2026-05-22T09:30:00Z"));

        String html = renderHtml(m);
        assertThat(html).contains("Landlord · approved 22 May 2026 11:30 SAST");
    }

    @Test
    void renderHtml_attested_agent_shows_attested_sast_stamp() {
        Mandate m = offlineMandate();
        m.setAgentAttested(true);
        setField(m, "createdAt", OffsetDateTime.parse("2026-05-20T08:15:00Z"));

        String html = renderHtml(m);
        assertThat(html).contains("Agent · attested 20 May 2026 10:15 SAST");
    }

    @Test
    void renderHtml_signed_states_render_cursive_overlay_initials() {
        // Landlord cursive overlay = first initial + ". Surname" once
        // either an offline signed PDF lands or an online ACTIVE flip
        // happens. Awaiting state renders an empty overlay.
        Mandate signed = offlineMandate();
        signed.setSignedDocumentPath("storage/signed.pdf");
        String html = renderHtml(signed);
        assertThat(html).contains(">T. Vilakazi<");

        Mandate awaiting = offlineMandate();
        String awaitingHtml = renderHtml(awaiting);
        assertThat(awaitingHtml).doesNotContain(">T. Vilakazi<");
    }

    @Test
    void renderHtml_wears_the_shared_specimen_chrome() {
        // Spot-checks the design-locked elements: rotated SPECIMEN
        // watermark, HABI/TAT wordmark + hex logo, slate body palette,
        // and the page-of footer with the hb.co.za short URL.
        String html = renderHtml(offlineMandate());
        assertThat(html).contains(">HABI<");
        assertThat(html).contains(">TAT<");
        assertThat(html).contains("SPECIMEN");
        assertThat(html).contains("Property Management Mandate");
        assertThat(html).contains("Mandate Agreement");
        assertThat(html).contains("rotate(-22deg)");
        assertThat(html).contains("Habitat SA (Pty) Ltd");
        assertThat(html).contains("PPRA reg. FFC2026/00831");
        assertThat(html).contains("hb.co.za/M/");
        assertThat(html).contains("Witness");
        assertThat(html).contains("HB-W-");
    }

    private String renderHtml(Mandate m) {
        return templates.renderHtml("mandate.html", service.buildVars(m));
    }

    private static Mandate offlineMandate() {
        User agent = User.builder().firstName("Lerato").surname("Mokoena")
                .email("lerato@habitat.co.za").build();
        Property property = withId(Property.builder().title("Olive Court")
                .addressLine("12 Olive Rd").suburb("Yeoville").city("Joburg")
                .postalCode("2198").build(), UUID.randomUUID());
        return withId(Mandate.builder()
                .property(property)
                .agent(agent)
                .offlineLandlordName("Thandi Vilakazi")
                .offlineLandlordEmail("thandi@example.co.za")
                .mandateType(MandateType.FULL_MANAGEMENT)
                .status(MandateStatus.PENDING_OFFLINE_SIGNATURE)
                .feePercent(new BigDecimal("8.5"))
                .build(), UUID.randomUUID());
    }

    private static User landlordUser() {
        return User.builder().firstName("Thandi").surname("Vilakazi")
                .email("thandi@example.co.za").build();
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

    private static void setField(Object entity, String name, Object value) {
        try {
            Field f = entity.getClass().getSuperclass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(entity, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

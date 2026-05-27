package com.habitat.api.service;

import com.habitat.api.entity.Lease;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Renders a {@link Lease} to a signed-lease PDF using the shared
 * {@link PdfTemplateService} pipeline. The template lives at
 * {@code resources/templates/lease.html} and mirrors the visual design
 * of {@code /lease-pdf} in habitat-ui — rotated SPECIMEN watermark, HABI/TAT
 * logo, slate-tinted hairlines, cursive signature overlay, and the page-of
 * footer with the {@code hb.co.za} short URL.
 *
 * <p>Inputs are the BUG-02 snapshots already on Lease (tenant /
 * landlord / unit / property / address) so a lease rendered after a
 * tenant changes their name shows the contracted name, not the
 * current one.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public final class LeasePdfService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy");
    private static final DateTimeFormatter STAMP_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm 'SAST'");
    private static final ZoneId SAST = ZoneId.of("Africa/Johannesburg");

    private final PdfTemplateService templates;

    public byte[] render(Lease lease) {
        return templates.renderToPdf("lease.html", buildVars(lease));
    }

    /** Package-private so tests can assert against the substituted HTML. */
    Map<String, String> buildVars(Lease lease) {
        Map<String, String> vars = new LinkedHashMap<>();
        String leaseRef = nullSafe(lease.getLeaseRef());
        vars.put("leaseRef", leaseRef);
        vars.put("generatedOn", DATE_FMT.format(OffsetDateTime.now().toLocalDate()));
        vars.put("landlord", nullSafe(lease.getLandlordNameSnapshot()));
        vars.put("tenant", nullSafe(lease.getTenantNameSnapshot()));
        vars.put("witnessRef", deriveWitnessRef(leaseRef));
        vars.put("propertyTitle", nullSafe(lease.getPropertyTitleSnapshot()));
        vars.put("unitTitle", nullSafe(lease.getUnitTitleSnapshot()));
        vars.put("propertyAddress", nullSafe(lease.getPropertyAddressSnapshot()));
        vars.put("startDate", formatDate(lease.getStartDate()));
        vars.put("monthlyRent", formatMoney(lease.getMonthlyRent()));
        vars.put("deposit", formatMoney(lease.getDeposit()));
        vars.put("termMonths", lease.getTermMonths() == null ? "—" : String.valueOf(lease.getTermMonths()));
        vars.put("tenantSigName", sigName(lease.getTenantSignedAt(), lease.getTenantNameSnapshot()));
        vars.put("landlordSigName", sigName(lease.getLandlordSignedAt(), lease.getLandlordNameSnapshot()));
        vars.put("tenantSigStamp", sigStamp("Tenant", lease.getTenantSignedAt()));
        vars.put("landlordSigStamp", sigStamp("Landlord", lease.getLandlordSignedAt()));
        vars.put("shortUrl", shortUrl("L", leaseRef));
        return vars;
    }

    /**
     * "Tenant · signed 4 May 2026 14:22 SAST" once signed, "Tenant ·
     * awaiting signature" before. Mirrors the slate-text caption pattern
     * under each signature line in the {@code /lease-pdf} design.
     */
    private static String sigStamp(String role, OffsetDateTime at) {
        if (at == null) return role + " · awaiting signature";
        return role + " · signed " + STAMP_FMT.format(at.atZoneSameInstant(SAST));
    }

    /**
     * Cursive signature glyph the renderer overlays on the signature
     * line — "Naledi Mokoena" → "N. Mokoena". Empty string for unsigned
     * parties so the line stays clean.
     */
    private static String sigName(OffsetDateTime at, String fullName) {
        if (at == null) return "";
        if (fullName == null || fullName.isBlank()) return "";
        String trimmed = fullName.trim();
        int sp = trimmed.indexOf(' ');
        if (sp <= 0) return trimmed;
        return trimmed.charAt(0) + ". " + trimmed.substring(sp + 1).trim();
    }

    /**
     * Witness ref derived from the lease ref's tail. Falls back to a
     * stable placeholder when the ref is "—" or short.
     */
    private static String deriveWitnessRef(String leaseRef) {
        if (leaseRef == null || leaseRef.length() < 5 || "—".equals(leaseRef)) return "HB-W-PENDING";
        return "HB-W-" + leaseRef.substring(Math.max(0, leaseRef.length() - 5)).toUpperCase();
    }

    /**
     * Footer short URL — the {@code hb.co.za/L/04891} pattern from the
     * design. Uses the lease ref's last segment as the slug.
     */
    private static String shortUrl(String prefix, String ref) {
        if (ref == null || ref.length() < 5 || "—".equals(ref)) return "hb.co.za/" + prefix + "/pending";
        return "hb.co.za/" + prefix + "/" + ref.substring(Math.max(0, ref.length() - 5)).toUpperCase();
    }

    private static String nullSafe(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private static String formatMoney(BigDecimal v) {
        if (v == null) return "—";
        return "R " + v.toPlainString();
    }

    private static String formatDate(LocalDate d) {
        return d == null ? "—" : DATE_FMT.format(d);
    }
}

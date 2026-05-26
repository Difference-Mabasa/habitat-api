package com.habitat.api.service;

import com.habitat.api.entity.Lease;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Renders a {@link Lease} to a signed-lease PDF using the shared
 * {@link PdfTemplateService} pipeline. The template lives at
 * {@code resources/templates/lease.html} so visual tweaks land in one
 * place — alongside the matching mandate / invoice templates.
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
            DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm 'UTC'");

    private final PdfTemplateService templates;

    public byte[] render(Lease lease) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("leaseRef", nullSafe(lease.getLeaseRef()));
        vars.put("generatedOn", DATE_FMT.format(OffsetDateTime.now().toLocalDate()));
        vars.put("landlord", nullSafe(lease.getLandlordNameSnapshot()));
        vars.put("tenant", nullSafe(lease.getTenantNameSnapshot()));
        vars.put("propertyTitle", nullSafe(lease.getPropertyTitleSnapshot()));
        vars.put("unitTitle", nullSafe(lease.getUnitTitleSnapshot()));
        vars.put("propertyAddress", nullSafe(lease.getPropertyAddressSnapshot()));
        vars.put("startDate", formatDate(lease.getStartDate()));
        vars.put("monthlyRent", formatMoney(lease.getMonthlyRent()));
        vars.put("deposit", formatMoney(lease.getDeposit()));
        vars.put("termMonths", lease.getTermMonths() == null ? "—" : String.valueOf(lease.getTermMonths()));
        vars.put("tenantSigStamp", formatStamp(lease.getTenantSignedAt()));
        vars.put("landlordSigStamp", formatStamp(lease.getLandlordSignedAt()));
        return templates.renderToPdf("lease.html", vars);
    }

    private static String nullSafe(String s) {
        return s == null ? "—" : s;
    }

    private static String formatMoney(BigDecimal v) {
        if (v == null) return "—";
        return "R " + v.toPlainString();
    }

    private static String formatDate(LocalDate d) {
        return d == null ? "—" : DATE_FMT.format(d);
    }

    private static String formatStamp(OffsetDateTime at) {
        if (at == null) return "Awaiting signature";
        return "Signed " + STAMP_FMT.format(at.toZonedDateTime());
    }
}

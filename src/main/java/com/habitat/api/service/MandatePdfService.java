package com.habitat.api.service;

import com.habitat.api.entity.Mandate;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.User;
import com.habitat.api.enums.MandateType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Renders a {@link Mandate} to a PDF the agent can email + the offline
 * landlord can sign on paper. Uses the shared
 * {@link PdfTemplateService} pipeline so the visual style stays in
 * lock-step with the lease + invoice templates.
 */
@Service
@RequiredArgsConstructor
public final class MandatePdfService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy");

    private final PdfTemplateService templates;

    public byte[] render(Mandate mandate) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("mandateRef", deriveRef(mandate.getId()));
        vars.put("generatedOn", DATE_FMT.format(OffsetDateTime.now().toLocalDate()));
        vars.put("landlordName", deriveLandlordName(mandate));
        vars.put("landlordEmail", deriveLandlordEmail(mandate));
        vars.put("agentName", displayName(mandate.getAgent()));
        Property p = mandate.getProperty();
        vars.put("propertyTitle", nullSafe(p == null ? null : p.getTitle()));
        vars.put("propertyAddress", formatAddress(p));
        vars.put("mandateTypeLabel", labelFor(mandate.getMandateType()));
        vars.put("feeLabel", formatFee(mandate.getFeePercent(), mandate.getMandateType()));
        return templates.renderToPdf("mandate.html", vars);
    }

    private static String deriveRef(UUID id) {
        if (id == null) return "HB-MAN-PENDING";
        String suffix = id.toString().replace("-", "").substring(0, 8).toUpperCase();
        return "HB-MAN-" + suffix;
    }

    private static String deriveLandlordName(Mandate m) {
        if (m.getLandlordUser() != null) return displayName(m.getLandlordUser());
        return nullSafe(m.getOfflineLandlordName());
    }

    private static String deriveLandlordEmail(Mandate m) {
        if (m.getLandlordUser() != null) return nullSafe(m.getLandlordUser().getEmail());
        return nullSafe(m.getOfflineLandlordEmail());
    }

    private static String labelFor(MandateType type) {
        if (type == null) return "—";
        return switch (type) {
            case FULL_MANAGEMENT -> "Full management";
            case TENANT_FIND -> "Tenant find only";
            case LETTING_AND_INSPECTIONS -> "Letting & inspections";
        };
    }

    private static String formatFee(BigDecimal pct, MandateType type) {
        if (pct == null) return "—";
        // Tenant-find mandates conventionally use a flat fee ("1 month
        // rent"); the wizard's mandate fee field is still % to match
        // backroom — interpret consistently here.
        return pct.toPlainString() + "% of rent collected";
    }

    private static String displayName(User u) {
        if (u == null) return "—";
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getSurname() == null ? "" : u.getSurname();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? nullSafe(u.getEmail()) : name;
    }

    private static String formatAddress(Property p) {
        if (p == null) return "—";
        StringBuilder b = new StringBuilder();
        if (p.getAddressLine() != null && !p.getAddressLine().isBlank()) {
            b.append(p.getAddressLine()).append(", ");
        }
        if (p.getSuburb() != null && !p.getSuburb().isBlank()) {
            b.append(p.getSuburb()).append(", ");
        }
        if (p.getCity() != null && !p.getCity().isBlank()) {
            b.append(p.getCity());
        }
        if (p.getPostalCode() != null && !p.getPostalCode().isBlank()) {
            b.append(" ").append(p.getPostalCode());
        }
        String out = b.toString().trim();
        if (out.endsWith(",")) out = out.substring(0, out.length() - 1).trim();
        return out.isEmpty() ? "—" : out;
    }

    private static String nullSafe(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}

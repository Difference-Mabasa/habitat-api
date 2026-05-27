package com.habitat.api.service;

import com.habitat.api.entity.Mandate;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.User;
import com.habitat.api.enums.MandateStatus;
import com.habitat.api.enums.MandateType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Renders a {@link Mandate} to a PDF the agent can email + the offline
 * landlord can sign on paper. Shares the {@code lease.html} visual
 * language: rotated SPECIMEN watermark, HABI/TAT logo, slate-tinted
 * hairlines, cursive signature overlay, page-of footer with the short
 * URL. Lives in the {@link PdfTemplateService} pipeline so style fixes
 * land in one place.
 */
@Service
@RequiredArgsConstructor
public final class MandatePdfService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy");
    private static final DateTimeFormatter STAMP_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm 'SAST'");
    private static final ZoneId SAST = ZoneId.of("Africa/Johannesburg");

    private final PdfTemplateService templates;

    public byte[] render(Mandate mandate) {
        return templates.renderToPdf("mandate.html", buildVars(mandate));
    }

    /**
     * Package-private so MandatePdfServiceTest can assert sig-stamp
     * branches against the substituted HTML without re-running the PDF
     * renderer.
     */
    Map<String, String> buildVars(Mandate mandate) {
        Map<String, String> vars = new LinkedHashMap<>();
        String mandateRef = deriveRef(mandate.getId());
        vars.put("mandateRef", mandateRef);
        vars.put("generatedOn", DATE_FMT.format(OffsetDateTime.now().toLocalDate()));
        String landlordName = deriveLandlordName(mandate);
        String agentName = displayName(mandate.getAgent());
        vars.put("landlordName", landlordName);
        vars.put("agentName", agentName);
        vars.put("witnessRef", deriveWitnessRef(mandateRef));
        Property p = mandate.getProperty();
        vars.put("propertyTitle", nullSafe(p == null ? null : p.getTitle()));
        vars.put("propertyAddress", formatAddress(p));
        vars.put("mandateTypeLabel", labelFor(mandate.getMandateType()));
        vars.put("feeLabel", formatFee(mandate.getFeePercent()));
        vars.put("landlordSigName", landlordSigName(mandate, landlordName));
        vars.put("landlordSigStamp", landlordSigStamp(mandate));
        vars.put("agentSigName", agentSigName(mandate, agentName));
        vars.put("agentSigStamp", agentSigStamp(mandate));
        vars.put("shortUrl", shortUrl(mandateRef));
        return vars;
    }

    /**
     * "Landlord · signed [date] SAST" once an offline signed PDF lands;
     * "Landlord · approved [date] SAST" once an online landlord taps
     * Approve and status flips to ACTIVE; "Landlord · awaiting
     * signature" until either of those happens.
     */
    private static String landlordSigStamp(Mandate m) {
        OffsetDateTime when = m.getUpdatedAt();
        if (m.getSignedDocumentPath() != null) {
            return "Landlord · signed " + formatStamp(when);
        }
        if (m.getLandlordUser() != null && m.getStatus() == MandateStatus.ACTIVE) {
            return "Landlord · approved " + formatStamp(when);
        }
        return "Landlord · awaiting signature";
    }

    /**
     * Agent stamp tracks the attestation flag — set when the agent
     * confirms in-app that they hold (or will hold) the signed
     * original. The createdAt is the closest issuance timestamp we have.
     */
    private static String agentSigStamp(Mandate m) {
        if (!m.isAgentAttested()) return "Agent · awaiting attestation";
        return "Agent · attested " + formatStamp(m.getCreatedAt());
    }

    /**
     * Cursive overlay above the signature line — first initial + ".
     * Surname". Empty when the party hasn't signed yet so the line
     * stays clean.
     */
    private static String landlordSigName(Mandate m, String landlordName) {
        boolean signed = m.getSignedDocumentPath() != null
                || (m.getLandlordUser() != null && m.getStatus() == MandateStatus.ACTIVE);
        return signed ? sigGlyph(landlordName) : "";
    }

    private static String agentSigName(Mandate m, String agentName) {
        return m.isAgentAttested() ? sigGlyph(agentName) : "";
    }

    private static String sigGlyph(String fullName) {
        if (fullName == null || fullName.isBlank() || "—".equals(fullName)) return "";
        String trimmed = fullName.trim();
        int sp = trimmed.indexOf(' ');
        if (sp <= 0) return trimmed;
        return trimmed.charAt(0) + ". " + trimmed.substring(sp + 1).trim();
    }

    private static String formatStamp(OffsetDateTime when) {
        OffsetDateTime instant = when == null ? OffsetDateTime.now() : when;
        return STAMP_FMT.format(instant.atZoneSameInstant(SAST));
    }

    private static String deriveRef(UUID id) {
        if (id == null) return "HB-MAN-PENDING";
        String suffix = id.toString().replace("-", "").substring(0, 8).toUpperCase();
        return "HB-MAN-" + suffix;
    }

    private static String deriveWitnessRef(String mandateRef) {
        if (mandateRef == null || mandateRef.length() < 5) return "HB-W-PENDING";
        return "HB-W-" + mandateRef.substring(Math.max(0, mandateRef.length() - 5)).toUpperCase();
    }

    private static String shortUrl(String mandateRef) {
        if (mandateRef == null || mandateRef.length() < 5) return "hb.co.za/M/pending";
        return "hb.co.za/M/" + mandateRef.substring(Math.max(0, mandateRef.length() - 5)).toUpperCase();
    }

    private static String deriveLandlordName(Mandate m) {
        if (m.getLandlordUser() != null) return displayName(m.getLandlordUser());
        return nullSafe(m.getOfflineLandlordName());
    }

    private static String labelFor(MandateType type) {
        if (type == null) return "—";
        return switch (type) {
            case FULL_MANAGEMENT -> "Full management";
            case TENANT_FIND -> "Tenant find only";
            case LETTING_AND_INSPECTIONS -> "Letting & inspections";
        };
    }

    private static String formatFee(BigDecimal pct) {
        if (pct == null) return "—";
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

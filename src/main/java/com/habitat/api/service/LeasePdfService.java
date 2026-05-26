package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.entity.Lease;
import com.habitat.api.exception.ServiceUnavailableException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Renders a {@link Lease} to a signed-lease PDF using openhtmltopdf.
 *
 * <p>Template is inline because there's only one shape today. When we
 * add the {@code RHA_SIX_MONTH} / {@code RHA_ROOM} variants this should
 * graduate to a Thymeleaf resource under {@code resources/templates/}.
 *
 * <p>Bytes come back as a byte array — leases are small (~10 KB) so
 * the memory cost is negligible and the API stays uniform with
 * {@code StorageService.store(byte[])} when the bytes are persisted.
 */
@Service
@Slf4j
public final class LeasePdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMMM yyyy");
    private static final DateTimeFormatter STAMP_FMT = DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm 'UTC'");

    public byte[] render(Lease lease) {
        String html = buildHtml(lease);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF render failed for lease {}", lease.getId(), e);
            throw new ServiceUnavailableException(ErrorMessages.SERVICE_UNAVAILABLE, e);
        }
    }

    private static String buildHtml(Lease lease) {
        String tenant = nullSafe(lease.getTenantNameSnapshot());
        String landlord = nullSafe(lease.getLandlordNameSnapshot());
        String propertyTitle = nullSafe(lease.getPropertyTitleSnapshot());
        String propertyAddress = nullSafe(lease.getPropertyAddressSnapshot());
        String unitTitle = nullSafe(lease.getUnitTitleSnapshot());
        String monthly = formatMoney(lease.getMonthlyRent());
        String deposit = formatMoney(lease.getDeposit());
        String term = lease.getTermMonths() == null ? "—" : String.valueOf(lease.getTermMonths());
        String start = formatDate(lease.getStartDate());
        String tenantSig = formatStamp(lease.getTenantSignedAt(), tenant);
        String landlordSig = formatStamp(lease.getLandlordSignedAt(), landlord);
        String leaseRef = nullSafe(lease.getLeaseRef());

        return """
            <html><head><style>
              @page { size: A4; margin: 24mm; }
              body { font-family: 'Helvetica', sans-serif; font-size: 11pt; color: #1a1a1a; line-height: 1.5; }
              h1 { font-size: 22pt; margin: 0 0 4mm 0; letter-spacing: -0.5px; }
              h2 { font-size: 13pt; margin: 8mm 0 2mm; border-bottom: 1px solid #ddd; padding-bottom: 2mm; }
              .ref { font-family: 'Courier', monospace; font-size: 10pt; color: #666; margin-bottom: 6mm; }
              table { width: 100%%; border-collapse: collapse; margin-top: 2mm; }
              td { padding: 2mm 0; vertical-align: top; }
              td.k { width: 35%%; color: #666; }
              td.v { font-weight: 500; }
              .sig { display: block; margin-top: 12mm; padding-top: 4mm; border-top: 1px solid #999; }
              .sig .by { font-weight: 600; }
              .sig .at { font-family: 'Courier', monospace; font-size: 9pt; color: #666; margin-top: 1mm; }
              .footer { margin-top: 14mm; font-size: 9pt; color: #999; text-align: center; }
            </style></head><body>
              <h1>Residential Lease Agreement</h1>
              <div class="ref">%s</div>

              <h2>Property</h2>
              <table>
                <tr><td class="k">Building</td><td class="v">%s</td></tr>
                <tr><td class="k">Unit</td><td class="v">%s</td></tr>
                <tr><td class="k">Address</td><td class="v">%s</td></tr>
              </table>

              <h2>Parties</h2>
              <table>
                <tr><td class="k">Tenant</td><td class="v">%s</td></tr>
                <tr><td class="k">Landlord</td><td class="v">%s</td></tr>
              </table>

              <h2>Financials</h2>
              <table>
                <tr><td class="k">Monthly rent</td><td class="v">%s</td></tr>
                <tr><td class="k">Security deposit</td><td class="v">%s</td></tr>
                <tr><td class="k">Term</td><td class="v">%s months</td></tr>
                <tr><td class="k">Start date</td><td class="v">%s</td></tr>
              </table>

              <h2>Signatures</h2>
              <div class="sig">
                <div class="by">Tenant — %s</div>
                <div class="at">%s</div>
              </div>
              <div class="sig">
                <div class="by">Landlord — %s</div>
                <div class="at">%s</div>
              </div>

              <div class="footer">Generated by Habitat · This PDF is the binding record of the lease above.</div>
            </body></html>
            """.formatted(
                escape(leaseRef),
                escape(propertyTitle),
                escape(unitTitle),
                escape(propertyAddress),
                escape(tenant),
                escape(landlord),
                escape(monthly),
                escape(deposit),
                escape(term),
                escape(start),
                escape(tenant),
                escape(tenantSig),
                escape(landlord),
                escape(landlordSig)
        );
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
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

    private static String formatStamp(OffsetDateTime at, String by) {
        if (at == null) return "Awaiting signature";
        return "Signed " + STAMP_FMT.format(at.toZonedDateTime());
    }
}

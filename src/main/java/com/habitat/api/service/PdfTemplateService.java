package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.exception.ServiceUnavailableException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Renders a classpath HTML template to PDF bytes via openhtmltopdf.
 *
 * <p>Templates live under {@code src/main/resources/templates/} and
 * use {@code {{key}}} placeholders. {@link #renderToPdf} substitutes
 * each placeholder with its (escaped) value, then runs the result
 * through openhtmltopdf.
 *
 * <p>All current habitat PDFs (lease, mandate, soon invoice) share
 * one template-loading pipeline so they pick up styling changes from
 * one place — {@code templates/_pdf-base.css} — and the structural
 * sections (header bar + section blocks + footer) stay aligned.
 *
 * <p>Substitution is plain string replace, not Thymeleaf, to keep the
 * dependency surface tight. Templates with control flow would force a
 * proper engine.
 */
@Service
@Slf4j
public final class PdfTemplateService {

    /**
     * Load a template from the classpath, substitute {@code {{key}}}
     * placeholders, and render to PDF bytes.
     *
     * @param templateName  filename under {@code templates/} (e.g. {@code "lease.html"}).
     * @param substitutions placeholder → value map. Values are HTML-escaped.
     */
    public byte[] renderToPdf(String templateName, Map<String, String> substitutions) {
        String html = renderHtml(templateName, substitutions);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF render failed for template {}", templateName, e);
            throw new ServiceUnavailableException(ErrorMessages.SERVICE_UNAVAILABLE, e);
        }
    }

    /**
     * Exposed for tests that want to inspect the rendered HTML without
     * the openhtmltopdf round-trip.
     */
    public String renderHtml(String templateName, Map<String, String> substitutions) {
        String template = loadTemplate(templateName);
        String result = template;
        // Replace each {{key}} with the HTML-escaped value. Iteration
        // order is irrelevant — placeholders don't overlap.
        for (Map.Entry<String, String> e : substitutions.entrySet()) {
            String placeholder = "{{" + e.getKey() + "}}";
            result = result.replace(placeholder, escape(e.getValue()));
        }
        return result;
    }

    private static String loadTemplate(String templateName) {
        ClassPathResource resource = new ClassPathResource("templates/" + templateName);
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ServiceUnavailableException(ErrorMessages.SERVICE_UNAVAILABLE, e);
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

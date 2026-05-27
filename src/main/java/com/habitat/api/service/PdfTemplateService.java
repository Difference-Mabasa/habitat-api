package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.exception.ServiceUnavailableException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Renders a classpath HTML template to PDF bytes via openhtmltopdf.
 *
 * <p>Templates live under {@code src/main/resources/templates/} and
 * use {@code {{key}}} placeholders. {@link #renderToPdf} substitutes
 * each placeholder with its (escaped) value, then runs the result
 * through openhtmltopdf with the bundled Inter / Anton / JetBrains Mono
 * fonts and inline SVG support — both required for visual parity with
 * the {@code /lease-pdf} design in habitat-ui.
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
     */
    public byte[] renderToPdf(String templateName, Map<String, String> substitutions) {
        String html = renderHtml(templateName, substitutions);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useSVGDrawer(new BatikSVGDrawer());
            registerFonts(builder);
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
        for (Map.Entry<String, String> e : substitutions.entrySet()) {
            String placeholder = "{{" + e.getKey() + "}}";
            result = result.replace(placeholder, escape(e.getValue()));
        }
        return result;
    }

    /**
     * Register the bundled fonts so openhtmltopdf can resolve
     * {@code font-family: 'Inter'}, {@code 'Anton'}, and {@code 'JetBrains Mono'}
     * directly to embedded glyph data — needed because PDFBox ships
     * only the 14 base fonts (Helvetica, Times, Courier, Symbol,
     * Zapf-Dingbats) and our templates explicitly target the design's
     * type families.
     */
    private static void registerFonts(PdfRendererBuilder builder) {
        useFont(builder, "fonts/Inter-Regular.ttf", "Inter", 400);
        useFont(builder, "fonts/Inter-SemiBold.ttf", "Inter", 600);
        useFont(builder, "fonts/Inter-Bold.ttf", "Inter", 700);
        useFont(builder, "fonts/Anton-Regular.ttf", "Anton", 400);
        useFont(builder, "fonts/JetBrainsMono-Regular.ttf", "JetBrains Mono", 400);
        useFont(builder, "fonts/JetBrainsMono-Bold.ttf", "JetBrains Mono", 700);
    }

    private static void useFont(PdfRendererBuilder builder, String classpathPath, String family, int weight) {
        Supplier<InputStream> source = () -> {
            try {
                return new ClassPathResource(classpathPath).getInputStream();
            } catch (IOException e) {
                throw new IllegalStateException("missing PDF font asset: " + classpathPath, e);
            }
        };
        builder.useFont(source::get, family, weight, PdfRendererBuilder.FontStyle.NORMAL, true);
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

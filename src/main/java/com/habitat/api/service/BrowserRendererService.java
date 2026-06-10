package com.habitat.api.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Margin;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Headless-Chromium PDF pipeline. Boots one Playwright + Browser
 * lazily on first render (Chromium download on first cold start —
 * ~150 MB, cached under {@code ~/.cache/ms-playwright}), then reuses
 * the browser for every render.
 *
 * <p>The renderer takes a relative path under
 * {@code app.ui.base-url}, navigates Chrome to it, waits for the
 * network to settle, and returns the PDF bytes that {@code page.pdf()}
 * produces — byte-identical to what the same Chrome would print from
 * the URL by hand. CSS {@code @page}, rotated watermarks, inline SVG,
 * web fonts, and proper z-stacking all work because Chrome is doing
 * the rendering.
 *
 * <p>Step 1 wires this to the dev-hub specimens; step 2 will move
 * production lease + mandate downloads onto it.
 */
@Service
@Slf4j
public final class BrowserRendererService {

    private final String uiBaseUrl;
    private volatile Playwright playwright;
    private volatile Browser browser;
    private final Object initLock = new Object();

    // The Playwright Java client is single-threaded: one shared Browser /
    // protocol Connection can only be driven by one thread at a time.
    // Concurrent renders (two detail screens fetching their PDF at once)
    // otherwise corrupt the connection's object registry — surfacing as
    // "Cannot find object to call __adopt__" / "Object doesn't exist" and
    // a 500. Serialise every render through this lock. Renders are
    // infrequent and short-lived, so queueing is an acceptable trade for
    // correctness; a pool of single-threaded renderers is the next step if
    // throughput ever demands it.
    private final ReentrantLock renderLock = new ReentrantLock();

    public BrowserRendererService(@Value("${app.ui.base-url}") String uiBaseUrl) {
        // Strip a trailing slash so callers can pass paths with a
        // leading "/" without worrying about a doubled separator.
        this.uiBaseUrl = uiBaseUrl.endsWith("/")
                ? uiBaseUrl.substring(0, uiBaseUrl.length() - 1)
                : uiBaseUrl;
    }

    /**
     * Render the page at {@code relativePath} (e.g.
     * {@code "/print/lease/specimen"}) to PDF bytes via headless
     * Chromium. Each call uses its own short-lived browser context so
     * cookies / localStorage from a prior render don't leak.
     */
    public byte[] renderUrlToPdf(String relativePath) {
        return renderUrlToPdf(relativePath, null);
    }

    /**
     * Render with the caller's {@code Authorization} header forwarded
     * to every request the headless browser makes — covers print routes
     * whose React tree fetches data from the API.
     *
     * <p>Step 2-proper will replace the forwarded JWT with a one-shot
     * signed token scoped to a single document; for step 1.5 we
     * forward the user's access token unchanged so the wizard's
     * mandate preview can land before the token plumbing arrives.
     */
    public byte[] renderUrlToPdf(String relativePath, String authorizationHeaderValue) {
        ensureInitialized();
        String url = uiBaseUrl + (relativePath.startsWith("/") ? relativePath : "/" + relativePath);
        renderLock.lock();
        try (BrowserContext ctx = browser.newContext();
             Page page = ctx.newPage()) {
            if (authorizationHeaderValue != null && !authorizationHeaderValue.isBlank()) {
                ctx.setExtraHTTPHeaders(Map.of("Authorization", authorizationHeaderValue));
            }
            page.navigate(url, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE)
                    .setTimeout(15_000));
            return page.pdf(new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setMargin(new Margin().setTop("0").setBottom("0").setLeft("0").setRight("0"))
                    .setPreferCSSPageSize(true));
        } finally {
            renderLock.unlock();
        }
    }

    private void ensureInitialized() {
        if (browser != null) return;
        synchronized (initLock) {
            if (browser != null) return;
            log.info("BrowserRendererService — booting headless Chromium (first render)");
            playwright = Playwright.create();
            browser = playwright.chromium().launch();
            log.info("BrowserRendererService — Chromium ready");
        }
    }

    @PreDestroy
    void close() {
        try {
            if (browser != null) browser.close();
        } finally {
            if (playwright != null) playwright.close();
        }
    }
}

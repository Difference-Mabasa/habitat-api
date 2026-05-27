package com.habitat.api.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure delegator — the service just maps each specimen to its print
 * route and hands the URL to {@link BrowserRendererService}. The actual
 * Chromium round-trip is covered by manual eyeballing of the dev-hub
 * preview; spinning up a real browser per unit test is too slow and
 * adds a network dependency (Playwright would need to download
 * Chromium on first run).
 */
class DevSpecimensServiceTest {

    private static final byte[] FAKE_PDF = "%PDF-1.4 specimen".getBytes();

    private final BrowserRendererService browser = mock(BrowserRendererService.class);
    private final DevSpecimensService service = new DevSpecimensService(browser);

    @Test
    void renderMandateSpecimen_prints_the_mandate_print_route() {
        when(browser.renderUrlToPdf("/print/mandate/specimen")).thenReturn(FAKE_PDF);

        assertThat(service.renderMandateSpecimen()).isEqualTo(FAKE_PDF);
        verify(browser).renderUrlToPdf("/print/mandate/specimen");
    }

    @Test
    void renderLeaseSpecimen_prints_the_lease_print_route() {
        when(browser.renderUrlToPdf("/print/lease/specimen")).thenReturn(FAKE_PDF);

        assertThat(service.renderLeaseSpecimen()).isEqualTo(FAKE_PDF);
        verify(browser).renderUrlToPdf("/print/lease/specimen");
    }

    @Test
    void renderInvoiceSpecimen_prints_the_invoice_print_route() {
        when(browser.renderUrlToPdf("/print/invoice/specimen")).thenReturn(FAKE_PDF);

        assertThat(service.renderInvoiceSpecimen()).isEqualTo(FAKE_PDF);
        verify(browser).renderUrlToPdf("/print/invoice/specimen");
    }
}

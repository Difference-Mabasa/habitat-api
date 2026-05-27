package com.habitat.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Dev-hub specimens. Points headless Chromium at the React
 * {@code /print/{lease,mandate}/specimen} routes — same design system
 * the on-screen {@code /lease-pdf} uses — and returns the byte-identical
 * PDF Chrome prints.
 *
 * <p>{@code @Profile("!prod")} keeps this off the production classpath;
 * it's only mounted from {@link com.habitat.api.controller.DevSpecimensController}.
 */
@Service
@RequiredArgsConstructor
@Profile("!prod")
public final class DevSpecimensService {

    private static final String MANDATE_SPECIMEN_PATH = "/print/mandate/specimen";
    private static final String LEASE_SPECIMEN_PATH = "/print/lease/specimen";
    private static final String INVOICE_SPECIMEN_PATH = "/print/invoice/specimen";

    private final BrowserRendererService browser;

    public byte[] renderMandateSpecimen() {
        return browser.renderUrlToPdf(MANDATE_SPECIMEN_PATH);
    }

    public byte[] renderLeaseSpecimen() {
        return browser.renderUrlToPdf(LEASE_SPECIMEN_PATH);
    }

    public byte[] renderInvoiceSpecimen() {
        return browser.renderUrlToPdf(INVOICE_SPECIMEN_PATH);
    }
}

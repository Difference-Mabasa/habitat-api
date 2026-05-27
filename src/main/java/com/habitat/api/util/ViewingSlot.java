package com.habitat.api.util;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Shared formatting for the viewing-slot date/time strings the
 * notification templates interpolate via {@code {viewingDate}} +
 * {@code {viewingTime}}. SAST is the canonical display zone (UTC+2,
 * no DST in SA).
 */
public final class ViewingSlot {

    private static final ZoneId SAST = ZoneId.of("Africa/Johannesburg");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm 'SAST'", Locale.ENGLISH);

    private ViewingSlot() {}

    public static String formatDate(OffsetDateTime when) {
        if (when == null) return "—";
        return DATE_FMT.format(when.atZoneSameInstant(SAST));
    }

    public static String formatTime(OffsetDateTime when) {
        if (when == null) return "—";
        return TIME_FMT.format(when.atZoneSameInstant(SAST));
    }
}

package com.habitat.api.util;

/**
 * SA ID number validation. The 13-digit RSA identity number ends in a
 * Luhn check digit computed over the first 12. Used as the dedup key
 * for offline landlord rows, so we want typos rejected at the API
 * boundary before they create phantom Landlord records.
 *
 * <p>Format: {@code YYMMDDSSSSCAZ} where {@code Z} is the Luhn
 * checksum. Doubling alternates from the right; doubled values &gt; 9
 * have their digits summed (standard Luhn).
 */
public final class SaIdNumber {

    private SaIdNumber() {}

    /** True when {@code candidate} is exactly 13 digits and passes the Luhn check. */
    public static boolean isValid(String candidate) {
        if (candidate == null) return false;
        if (candidate.length() != 13) return false;
        for (int i = 0; i < 13; i++) {
            if (!Character.isDigit(candidate.charAt(i))) return false;
        }
        int sum = 0;
        boolean doubleNext = false;
        for (int i = 12; i >= 0; i--) {
            int n = candidate.charAt(i) - '0';
            if (doubleNext) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            doubleNext = !doubleNext;
        }
        return sum % 10 == 0;
    }
}

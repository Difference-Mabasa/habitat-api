package com.habitat.api.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SaIdNumberTest {

    @Test
    void valid_sa_id_passes_the_luhn_check() {
        // A real-shape SA ID — date 1980/01/01, sequence 5009 (male SA),
        // check digit computed via Luhn. Pre-computed.
        assertThat(SaIdNumber.isValid("8001015009087")).isTrue();
    }

    @Test
    void wrong_check_digit_fails() {
        assertThat(SaIdNumber.isValid("8001015009088")).isFalse();
    }

    @Test
    void non_thirteen_digits_fails() {
        assertThat(SaIdNumber.isValid("123")).isFalse();
        assertThat(SaIdNumber.isValid("12345678901234")).isFalse();
        assertThat(SaIdNumber.isValid("")).isFalse();
    }

    @Test
    void non_digit_chars_fail() {
        // 13 chars but containing a letter — still no.
        assertThat(SaIdNumber.isValid("80010150090A7")).isFalse();
    }

    @Test
    void null_input_fails() {
        assertThat(SaIdNumber.isValid(null)).isFalse();
    }
}

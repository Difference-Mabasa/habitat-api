package com.habitat.api.util;

import com.habitat.api.constants.TemplatePlaceholders;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateUtilsTest {

    @Test
    void substitutes_a_single_named_token() {
        String out = TemplateUtils.format(
                "Hi {firstName}!",
                TemplatePlaceholders.P_FIRST_NAME, "Sipho");
        assertThat(out).isEqualTo("Hi Sipho!");
    }

    @Test
    void substitutes_multiple_tokens() {
        String out = TemplateUtils.format(
                "Hi {firstName} {surname}.",
                TemplatePlaceholders.P_FIRST_NAME, "Sipho",
                TemplatePlaceholders.P_SURNAME, "Dlamini");
        assertThat(out).isEqualTo("Hi Sipho Dlamini.");
    }

    @Test
    void substitutes_null_value_as_empty_string() {
        String out = TemplateUtils.format(
                "Hi {firstName}!",
                TemplatePlaceholders.P_FIRST_NAME, null);
        assertThat(out).isEqualTo("Hi !");
    }

    @Test
    void leaves_unknown_tokens_alone() {
        String out = TemplateUtils.format(
                "Hi {firstName} from {city}",
                TemplatePlaceholders.P_FIRST_NAME, "Sipho");
        assertThat(out).isEqualTo("Hi Sipho from {city}");
    }

    @Test
    void rejects_odd_number_of_args() {
        assertThatThrownBy(() -> TemplateUtils.format(
                "x", "key1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

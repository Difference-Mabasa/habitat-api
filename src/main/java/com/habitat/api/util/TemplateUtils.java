package com.habitat.api.util;

import com.habitat.api.constants.TemplatePlaceholders;

/**
 * Tiny named-placeholder substitution for user-facing template strings.
 *
 * Use this — not {@code String.format} — for any string that will ever
 * be shown to a user. Positional {@code %s} indices silently break when a
 * translator reorders sentence elements; named tokens stay stable.
 *
 * Usage:
 * <pre>
 *   String body = TemplateUtils.format(
 *       NotificationMessages.WELCOME_BODY,
 *       TemplatePlaceholders.P_FIRST_NAME, user.getFirstName());
 * </pre>
 *
 * The keys + values are alternating varargs. Keys are bare (no braces);
 * we wrap them with {@link TemplatePlaceholders#wrap} before substitution.
 */
public final class TemplateUtils {

    public static String format(String template, String... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "keyValuePairs must contain alternating keys and values; got "
                            + keyValuePairs.length + " arguments");
        }
        String result = template;
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            String key = keyValuePairs[i];
            String value = keyValuePairs[i + 1] == null ? "" : keyValuePairs[i + 1];
            result = result.replace(TemplatePlaceholders.wrap(key), value);
        }
        return result;
    }

    private TemplateUtils() {}
}

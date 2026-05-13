package com.habitat.api.constants;

/**
 * Placeholder tokens used inside templated user-facing strings
 * (notification titles + bodies, email subjects + bodies). Lets us swap
 * the format style (currently {@code "{firstName}"}) in one place without
 * touching every consumer.
 *
 * Use with {@link com.habitat.api.util.TemplateUtils#format} — never use
 * {@code String.format} for user-facing messages (positional indices
 * silently break when a translator reorders).
 */
public final class TemplatePlaceholders {

    /** Wraps a key in the template delimiters. */
    public static String wrap(String key) {
        return "{" + key + "}";
    }

    public static final String P_FIRST_NAME = "firstName";
    public static final String P_SURNAME = "surname";

    private TemplatePlaceholders() {}
}

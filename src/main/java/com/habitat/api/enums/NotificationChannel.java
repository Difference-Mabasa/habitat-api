package com.habitat.api.enums;

/**
 * Where a notification can be delivered. {@link #IN_APP} is the only
 * channel that materialises as a row in {@code notifications} (and so
 * shows up in the bell drawer); EMAIL and SMS are out-of-band sends
 * routed to provider stubs in this slice.
 */
public enum NotificationChannel {
    IN_APP,
    EMAIL,
    SMS
}

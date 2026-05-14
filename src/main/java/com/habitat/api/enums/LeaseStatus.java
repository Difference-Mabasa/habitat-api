package com.habitat.api.enums;

/**
 * Lifecycle of a residential lease agreement. Created the moment the
 * tenant pays the deposit invoice; sits in {@code PENDING_SIGNATURES}
 * until both parties have signed. Move-in lands in slice 4.
 */
public enum LeaseStatus {
    PENDING_SIGNATURES,
    SIGNED,
    COMPLETED,
    DECLINED,
    VOIDED
}

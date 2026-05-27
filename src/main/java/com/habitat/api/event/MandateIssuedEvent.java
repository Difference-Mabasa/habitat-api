package com.habitat.api.event;

import java.util.UUID;

/**
 * Published by {@link com.habitat.api.service.MandateService#issue}
 * once a mandate row is persisted. The listener branches on the
 * resolved landlord type:
 *
 * <ul>
 *   <li>ONLINE → push MANDATE_PENDING_LANDLORD_APPROVAL to the
 *       landlord-User. They approve/reject from /my-mandates.</li>
 *   <li>OFFLINE → push MANDATE_PENDING_OFFLINE_SIGNATURE to the
 *       agent themselves — the landlord has no Habitat account to
 *       push to, so the agent gets the reminder to email/hand them
 *       the signable PDF.</li>
 * </ul>
 */
public record MandateIssuedEvent(UUID mandateId) {}

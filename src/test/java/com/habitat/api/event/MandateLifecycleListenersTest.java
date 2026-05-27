package com.habitat.api.event;

import com.habitat.api.dto.notification.PushResult;
import com.habitat.api.entity.Landlord;
import com.habitat.api.entity.Mandate;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.User;
import com.habitat.api.enums.LandlordType;
import com.habitat.api.enums.MandateStatus;
import com.habitat.api.enums.MandateType;
import com.habitat.api.enums.NotificationType;
import com.habitat.api.repository.MandateRepository;
import com.habitat.api.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Coverage for the four mandate-state listeners — issued, approved,
 * rejected, active. Each listener is exercised in isolation; cross-
 * event flows (approve → active) are covered by MandateServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class MandateLifecycleListenersTest {

    @Mock MandateRepository mandates;
    @Mock NotificationService notifications;

    @InjectMocks MandateIssuedListener issuedListener;
    @InjectMocks MandateApprovedListener approvedListener;
    @InjectMocks MandateRejectedListener rejectedListener;
    @InjectMocks MandateActiveListener activeListener;

    private static final UUID MANDATE_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID PROP_ID    = UUID.fromString("ffffffff-eeee-dddd-cccc-bbbbbbbbbbbb");
    private static final UUID AGENT_ID   = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OWNER_ID   = UUID.fromString("22222222-2222-2222-2222-222222222222");

    // ── MandateIssuedListener ────────────────────────────────────────

    @Test
    void issued_online_pushes_pending_approval_to_landlord_with_my_mandates_cta() {
        User agent = user(AGENT_ID, "Pieter", "Agent");
        User owner = user(OWNER_ID, "Naledi", "Owner");
        Mandate mandate = mandate(agent, onlineLandlord(owner), MandateStatus.PENDING_LANDLORD_APPROVAL);
        when(mandates.findById(MANDATE_ID)).thenReturn(Optional.of(mandate));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        issuedListener.onMandateIssued(new MandateIssuedEvent(MANDATE_ID));

        verify(notifications).push(
                eq(owner),
                eq(NotificationType.MANDATE_PENDING_LANDLORD_APPROVAL),
                any(), any(),
                eq("/mandate-approvals"),
                any(),
                eq(MANDATE_ID));
        verify(notifications, times(1))
                .push(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void issued_offline_pushes_signature_reminder_to_agent() {
        User agent = user(AGENT_ID, "Pieter", "Agent");
        Landlord offline = Landlord.builder()
                .type(LandlordType.OFFLINE)
                .firstName("Naledi").lastName("Owner")
                .email("naledi@example.co.za")
                .build();
        Mandate mandate = mandate(agent, offline, MandateStatus.PENDING_OFFLINE_SIGNATURE);
        when(mandates.findById(MANDATE_ID)).thenReturn(Optional.of(mandate));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        issuedListener.onMandateIssued(new MandateIssuedEvent(MANDATE_ID));

        verify(notifications).push(
                eq(agent),
                eq(NotificationType.MANDATE_PENDING_OFFLINE_SIGNATURE),
                any(), any(),
                contains("/property/" + PROP_ID + "?ctx=agent"),
                any(),
                eq(MANDATE_ID));
    }

    @Test
    void issued_skips_when_landlord_missing() {
        User agent = user(AGENT_ID, "Pieter", "Agent");
        Mandate mandate = mandate(agent, null, MandateStatus.PENDING_OFFLINE_SIGNATURE);
        when(mandates.findById(MANDATE_ID)).thenReturn(Optional.of(mandate));

        issuedListener.onMandateIssued(new MandateIssuedEvent(MANDATE_ID));

        verify(notifications, never())
                .push(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void issued_skips_when_mandate_vanishes() {
        when(mandates.findById(MANDATE_ID)).thenReturn(Optional.empty());
        issuedListener.onMandateIssued(new MandateIssuedEvent(MANDATE_ID));
        verify(notifications, never())
                .push(any(), any(), any(), any(), any(), any(), any());
    }

    // ── MandateApprovedListener ──────────────────────────────────────

    @Test
    void approved_pushes_only_to_agent() {
        User agent = user(AGENT_ID, "Pieter", "Agent");
        User owner = user(OWNER_ID, "Naledi", "Owner");
        Mandate mandate = mandate(agent, onlineLandlord(owner), MandateStatus.ACTIVE);
        when(mandates.findById(MANDATE_ID)).thenReturn(Optional.of(mandate));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        approvedListener.onMandateApproved(new MandateApprovedEvent(MANDATE_ID));

        verify(notifications).push(
                eq(agent),
                eq(NotificationType.MANDATE_APPROVED),
                any(), any(),
                eq("/property/" + PROP_ID + "?ctx=agent"),
                any(),
                eq(MANDATE_ID));
        verify(notifications, times(1))
                .push(any(), any(), any(), any(), any(), any(), any());
    }

    // ── MandateRejectedListener ──────────────────────────────────────

    @Test
    void rejected_pushes_only_to_agent() {
        User agent = user(AGENT_ID, "Pieter", "Agent");
        User owner = user(OWNER_ID, "Naledi", "Owner");
        Mandate mandate = mandate(agent, onlineLandlord(owner), MandateStatus.REJECTED);
        when(mandates.findById(MANDATE_ID)).thenReturn(Optional.of(mandate));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        rejectedListener.onMandateRejected(new MandateRejectedEvent(MANDATE_ID));

        verify(notifications).push(
                eq(agent),
                eq(NotificationType.MANDATE_REJECTED),
                any(), any(),
                eq("/property/" + PROP_ID + "?ctx=agent"),
                any(),
                eq(MANDATE_ID));
    }

    // ── MandateActiveListener ────────────────────────────────────────

    @Test
    void active_pushes_to_agent_and_online_landlord() {
        User agent = user(AGENT_ID, "Pieter", "Agent");
        User owner = user(OWNER_ID, "Naledi", "Owner");
        Mandate mandate = mandate(agent, onlineLandlord(owner), MandateStatus.ACTIVE);
        when(mandates.findById(MANDATE_ID)).thenReturn(Optional.of(mandate));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        activeListener.onMandateActive(new MandateActiveEvent(MANDATE_ID));

        verify(notifications).push(
                eq(agent),
                eq(NotificationType.MANDATE_ACTIVE),
                any(), any(),
                eq("/property/" + PROP_ID + "?ctx=agent"),
                any(),
                eq(MANDATE_ID));
        verify(notifications).push(
                eq(owner),
                eq(NotificationType.MANDATE_ACTIVE),
                any(), any(),
                eq("/property/" + PROP_ID + "?ctx=landlord"),
                any(),
                eq(MANDATE_ID));
    }

    @Test
    void active_skips_landlord_push_when_offline() {
        User agent = user(AGENT_ID, "Pieter", "Agent");
        Landlord offline = Landlord.builder()
                .type(LandlordType.OFFLINE)
                .firstName("Naledi").lastName("Owner")
                .build();
        Mandate mandate = mandate(agent, offline, MandateStatus.ACTIVE);
        when(mandates.findById(MANDATE_ID)).thenReturn(Optional.of(mandate));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        activeListener.onMandateActive(new MandateActiveEvent(MANDATE_ID));

        verify(notifications, times(1))
                .push(eq(agent), any(), any(), any(), any(), any(), eq(MANDATE_ID));
        verify(notifications, times(1))
                .push(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void active_skips_when_mandate_no_longer_active() {
        // Race-condition guard: a withdraw / re-issue could have moved
        // the row out of ACTIVE between the event firing and the
        // listener running. Don't claim "your mandate is now active"
        // when it isn't.
        User agent = user(AGENT_ID, "Pieter", "Agent");
        Mandate mandate = mandate(agent, onlineLandlord(user(OWNER_ID, "N", "O")),
                MandateStatus.REJECTED);
        when(mandates.findById(MANDATE_ID)).thenReturn(Optional.of(mandate));

        activeListener.onMandateActive(new MandateActiveEvent(MANDATE_ID));

        verify(notifications, never())
                .push(any(), any(), any(), any(), any(), any(), any());
    }

    // ── helpers ──────────────────────────────────────────────────────

    private static Landlord onlineLandlord(User u) {
        return Landlord.builder().type(LandlordType.ONLINE).user(u).build();
    }

    private static Mandate mandate(User agent, Landlord landlord, MandateStatus status) {
        Property property = withId(Property.builder()
                .title("Sandton Villa")
                .landlord(landlord)
                .manager(agent)
                .build(), PROP_ID);
        return withId(Mandate.builder()
                .property(property)
                .agent(agent)
                .mandateType(MandateType.FULL_MANAGEMENT)
                .status(status)
                .feePercent(new BigDecimal("8.0"))
                .build(), MANDATE_ID);
    }

    private static User user(UUID id, String first, String last) {
        User u = User.builder()
                .firstName(first).surname(last)
                .email((first + "@example.co.za").toLowerCase())
                .build();
        setId(u, id);
        return u;
    }

    private static <T> T withId(T entity, UUID id) {
        setId(entity, id);
        return entity;
    }

    private static void setId(Object entity, UUID id) {
        try {
            Field f = entity.getClass().getSuperclass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package com.habitat.api.event;

import com.habitat.api.dto.notification.PushResult;
import com.habitat.api.entity.Landlord;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.User;
import com.habitat.api.enums.LandlordType;
import com.habitat.api.enums.NotificationType;
import com.habitat.api.enums.PropertyStatus;
import com.habitat.api.repository.PropertyRepository;
import com.habitat.api.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyPublishedListenerTest {

    @Mock PropertyRepository properties;
    @Mock NotificationService notifications;
    @InjectMocks PropertyPublishedListener listener;

    private static final UUID PROP_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID MGR_ID  = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OWNER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void notifies_only_manager_when_landlord_direct() {
        // LANDLORD_DIRECT — manager IS the owner. The "owner" push
        // is suppressed when manager.id equals landlord.user.id so
        // we don't fire two notifications for the same person.
        User owner = user(MGR_ID, "Naledi", "Mokoena");
        Landlord landlord = onlineLandlord(owner);
        Property property = withId(Property.builder()
                .manager(owner)
                .landlord(landlord)
                .title("Sandton Villa")
                .status(PropertyStatus.LISTED)
                .build(), PROP_ID);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(property));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        listener.onPropertyPublished(new PropertyPublishedEvent(PROP_ID));

        verify(notifications, times(1))
                .push(eq(owner), eq(NotificationType.PROPERTY_PUBLISHED),
                        any(), any(), any(), any(), eq(PROP_ID));
    }

    @Test
    void notifies_manager_and_owner_when_agent_managed_with_online_owner() {
        User agent = user(MGR_ID, "Pieter", "Agent");
        User owner = user(OWNER_ID, "Naledi", "Owner");
        Landlord landlord = onlineLandlord(owner);
        Property property = withId(Property.builder()
                .manager(agent)
                .landlord(landlord)
                .title("Sandton Villa")
                .status(PropertyStatus.LISTED)
                .build(), PROP_ID);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(property));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        listener.onPropertyPublished(new PropertyPublishedEvent(PROP_ID));

        verify(notifications)
                .push(eq(agent), eq(NotificationType.PROPERTY_PUBLISHED),
                        any(), any(), any(), any(), eq(PROP_ID));
        verify(notifications)
                .push(eq(owner), eq(NotificationType.PROPERTY_PUBLISHED),
                        any(), any(), any(), any(), eq(PROP_ID));
    }

    @Test
    void notifies_only_manager_when_owner_is_offline() {
        // OFFLINE owners have no Habitat user — only the agent
        // (manager) is notified.
        User agent = user(MGR_ID, "Pieter", "Agent");
        Landlord offline = Landlord.builder()
                .type(LandlordType.OFFLINE)
                .firstName("Naledi").lastName("Owner")
                .email("naledi@example.co.za")
                .build();
        Property property = withId(Property.builder()
                .manager(agent)
                .landlord(offline)
                .title("Sandton Villa")
                .status(PropertyStatus.LISTED)
                .build(), PROP_ID);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(property));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        listener.onPropertyPublished(new PropertyPublishedEvent(PROP_ID));

        verify(notifications, times(1))
                .push(any(), eq(NotificationType.PROPERTY_PUBLISHED),
                        any(), any(), any(), any(), eq(PROP_ID));
    }

    @Test
    void skips_when_property_no_longer_listed() {
        // Race-condition guard: the property may have been unlisted
        // between publish-commit and listener-fire. Don't send the
        // "your listing is live" notification in that case.
        User owner = user(MGR_ID, "Naledi", "Mokoena");
        Property property = withId(Property.builder()
                .manager(owner)
                .landlord(onlineLandlord(owner))
                .title("Sandton Villa")
                .status(PropertyStatus.UNLISTED)
                .build(), PROP_ID);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(property));

        listener.onPropertyPublished(new PropertyPublishedEvent(PROP_ID));

        verify(notifications, never())
                .push(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void skips_when_property_vanished() {
        when(properties.findById(PROP_ID)).thenReturn(Optional.empty());
        listener.onPropertyPublished(new PropertyPublishedEvent(PROP_ID));
        verify(notifications, never())
                .push(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void cta_url_is_the_public_property_route() {
        User owner = user(MGR_ID, "Naledi", "Mokoena");
        Property property = withId(Property.builder()
                .manager(owner)
                .landlord(onlineLandlord(owner))
                .title("Sandton Villa")
                .status(PropertyStatus.LISTED)
                .build(), PROP_ID);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(property));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        listener.onPropertyPublished(new PropertyPublishedEvent(PROP_ID));

        verify(notifications).push(
                eq(owner), eq(NotificationType.PROPERTY_PUBLISHED),
                any(), any(),
                eq("/property/" + PROP_ID), any(), eq(PROP_ID));
    }

    // ── helpers ──────────────────────────────────────────────────────

    private static Landlord onlineLandlord(User u) {
        return Landlord.builder().type(LandlordType.ONLINE).user(u).build();
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

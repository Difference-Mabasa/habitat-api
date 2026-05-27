package com.habitat.api.service;

import com.habitat.api.dto.landlord.LandlordLookupResponse;
import com.habitat.api.entity.Landlord;
import com.habitat.api.entity.User;
import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.LandlordType;
import com.habitat.api.exception.BadRequestException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.repository.LandlordRepository;
import com.habitat.api.repository.UserRepository;
import com.habitat.api.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LandlordServiceTest {

    @Mock LandlordRepository landlords;
    @Mock UserRepository users;
    @Mock SecurityUtils security;
    @InjectMocks LandlordService service;

    private static final String VALID_ID = "8001015009087";
    private static final UUID AGENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OWNER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID LANDLORD_ROW_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    // ── lookupByIdNumber ─────────────────────────────────────────────

    @Test
    void lookup_returns_notFound_when_id_is_blank() {
        LandlordLookupResponse out = service.lookupByIdNumber("  ");
        assertThat(out.exists()).isFalse();
        verify(landlords, never()).findByIdNumber(any());
    }

    @Test
    void lookup_throws_when_id_fails_luhn_check() {
        assertThatThrownBy(() -> service.lookupByIdNumber("8001015009088"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void lookup_returns_existing_offline_landlord_with_ownedByMe_for_creator() {
        User agent = userWithId(AGENT_ID);
        Landlord existing = withId(Landlord.builder()
                .type(LandlordType.OFFLINE)
                .createdByAgent(agent)
                .idNumber(VALID_ID)
                .firstName("Thandi").lastName("Vilakazi")
                .email("thandi@example.co.za")
                .build(), LANDLORD_ROW_ID);
        when(landlords.findByIdNumber(VALID_ID)).thenReturn(Optional.of(existing));
        when(security.currentUserId()).thenReturn(Optional.of(AGENT_ID));

        LandlordLookupResponse out = service.lookupByIdNumber(VALID_ID);

        assertThat(out.exists()).isTrue();
        assertThat(out.id()).isEqualTo(LANDLORD_ROW_ID);
        assertThat(out.type()).isEqualTo(LandlordType.OFFLINE);
        assertThat(out.hasUserAccount()).isFalse();
        assertThat(out.firstName()).isEqualTo("Thandi");
        assertThat(out.lastName()).isEqualTo("Vilakazi");
        assertThat(out.ownedByMe()).isTrue();
    }

    @Test
    void lookup_marks_offline_landlord_as_not_owned_when_caller_isnt_creator() {
        User agentA = userWithId(AGENT_ID);
        Landlord existing = withId(Landlord.builder()
                .type(LandlordType.OFFLINE)
                .createdByAgent(agentA)
                .idNumber(VALID_ID)
                .firstName("Thandi").lastName("Vilakazi")
                .build(), LANDLORD_ROW_ID);
        when(landlords.findByIdNumber(VALID_ID)).thenReturn(Optional.of(existing));
        // Different agent calling the lookup.
        when(security.currentUserId()).thenReturn(Optional.of(UUID.randomUUID()));

        LandlordLookupResponse out = service.lookupByIdNumber(VALID_ID);

        assertThat(out.exists()).isTrue();
        assertThat(out.ownedByMe()).isFalse();
    }

    @Test
    void lookup_returns_notFound_when_no_row_for_id() {
        when(landlords.findByIdNumber(VALID_ID)).thenReturn(Optional.empty());
        // currentUserId not consulted on the empty path — leave unstubbed.

        LandlordLookupResponse out = service.lookupByIdNumber(VALID_ID);
        assertThat(out.exists()).isFalse();
    }

    // ── findOrCreateForUser ──────────────────────────────────────────

    @Test
    void findOrCreateForUser_returns_existing_row_when_one_exists() {
        User owner = userWithId(OWNER_ID);
        Landlord existing = withId(Landlord.builder()
                .type(LandlordType.ONLINE).user(owner).build(), LANDLORD_ROW_ID);
        when(landlords.findByUser_Id(OWNER_ID)).thenReturn(Optional.of(existing));

        Landlord out = service.findOrCreateForUser(owner);

        assertThat(out).isSameAs(existing);
        verify(landlords, never()).save(any());
    }

    @Test
    void findOrCreateForUser_creates_online_row_when_missing() {
        User owner = userWithId(OWNER_ID);
        when(landlords.findByUser_Id(OWNER_ID)).thenReturn(Optional.empty());
        when(landlords.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Landlord out = service.findOrCreateForUser(owner);

        assertThat(out.getType()).isEqualTo(LandlordType.ONLINE);
        assertThat(out.getUser()).isSameAs(owner);
        assertThat(out.getCreatedByAgent()).isNull();
    }

    // ── resolveForMandate ────────────────────────────────────────────

    @Test
    void resolveForMandate_returns_dedup_hit_when_id_matches_existing() {
        Landlord existing = withId(Landlord.builder()
                .type(LandlordType.OFFLINE).idNumber(VALID_ID).build(), LANDLORD_ROW_ID);
        when(landlords.findByIdNumber(VALID_ID)).thenReturn(Optional.of(existing));

        var capture = new LandlordService.MandateLandlordCapture(
                VALID_ID, "Thandi", "Vilakazi", "thandi@example.co.za", "0820000000");

        Landlord out = service.resolveForMandate(capture);

        assertThat(out).isSameAs(existing);
        verify(landlords, never()).save(any());
        verify(users, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void resolveForMandate_rejects_invalid_id_number() {
        var capture = new LandlordService.MandateLandlordCapture(
                "8001015009088", "Thandi", "Vilakazi", "thandi@example.co.za", null);

        assertThatThrownBy(() -> service.resolveForMandate(capture))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void resolveForMandate_creates_online_when_email_matches_user_and_no_id_match() {
        User userByEmail = userWithId(OWNER_ID);
        when(landlords.findByUser_Id(OWNER_ID)).thenReturn(Optional.empty());
        when(landlords.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(users.findByEmailIgnoreCase("thandi@example.co.za")).thenReturn(Optional.of(userByEmail));

        var capture = new LandlordService.MandateLandlordCapture(
                null, "Thandi", "Vilakazi", "thandi@example.co.za", null);

        Landlord out = service.resolveForMandate(capture);

        assertThat(out.getType()).isEqualTo(LandlordType.ONLINE);
        assertThat(out.getUser()).isSameAs(userByEmail);
    }

    @Test
    void resolveForMandate_creates_offline_when_email_matches_no_user() {
        User agent = userWithId(AGENT_ID);
        when(users.findByEmailIgnoreCase("thandi@example.co.za")).thenReturn(Optional.empty());
        when(security.requireUserId()).thenReturn(AGENT_ID);
        when(users.findById(AGENT_ID)).thenReturn(Optional.of(agent));
        when(landlords.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var capture = new LandlordService.MandateLandlordCapture(
                VALID_ID, "Thandi", "Vilakazi", "thandi@example.co.za", "0820000000");

        ArgumentCaptor<Landlord> captor = ArgumentCaptor.forClass(Landlord.class);
        Landlord out = service.resolveForMandate(capture);
        verify(landlords).save(captor.capture());

        Landlord saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(LandlordType.OFFLINE);
        assertThat(saved.getCreatedByAgent()).isSameAs(agent);
        assertThat(saved.getIdNumber()).isEqualTo(VALID_ID);
        assertThat(saved.getFirstName()).isEqualTo("Thandi");
        assertThat(saved.getLastName()).isEqualTo("Vilakazi");
        assertThat(saved.getEmail()).isEqualTo("thandi@example.co.za");
        assertThat(saved.getPhone()).isEqualTo("0820000000");
        assertThat(out.getType()).isEqualTo(LandlordType.OFFLINE);
    }

    @Test
    void resolveForMandate_rejects_when_offline_capture_is_missing_required_fields() {
        when(users.findByEmailIgnoreCase("thandi@example.co.za")).thenReturn(Optional.empty());

        var capture = new LandlordService.MandateLandlordCapture(
                null, null, null, "thandi@example.co.za", null);

        assertThatThrownBy(() -> service.resolveForMandate(capture))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void resolveForMandate_rejects_when_email_is_missing_and_no_dedup_hit() {
        var capture = new LandlordService.MandateLandlordCapture(
                null, "Thandi", "Vilakazi", null, null);

        assertThatThrownBy(() -> service.resolveForMandate(capture))
                .isInstanceOf(BadRequestException.class);
    }

    // ── requireCanEditDetails ────────────────────────────────────────

    @Test
    void requireCanEditDetails_allows_creator_agent_on_offline_row() {
        User agent = userWithId(AGENT_ID);
        Landlord row = Landlord.builder()
                .type(LandlordType.OFFLINE).createdByAgent(agent).build();
        when(security.isPrivileged()).thenReturn(false);
        when(security.currentUserId()).thenReturn(Optional.of(AGENT_ID));

        service.requireCanEditDetails(row); // no throw
    }

    @Test
    void requireCanEditDetails_blocks_non_creator_on_offline_row() {
        User creator = userWithId(AGENT_ID);
        Landlord row = Landlord.builder()
                .type(LandlordType.OFFLINE).createdByAgent(creator).build();
        when(security.isPrivileged()).thenReturn(false);
        when(security.currentUserId()).thenReturn(Optional.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.requireCanEditDetails(row))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireCanEditDetails_blocks_agent_on_online_row() {
        // ONLINE rows are edited by the user via account settings —
        // an agent should never be the writer.
        Landlord row = Landlord.builder()
                .type(LandlordType.ONLINE)
                .user(userWithId(OWNER_ID))
                .build();
        when(security.isPrivileged()).thenReturn(false);
        when(security.currentUserId()).thenReturn(Optional.of(AGENT_ID));

        assertThatThrownBy(() -> service.requireCanEditDetails(row))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireCanEditDetails_admin_always_allowed() {
        Landlord row = Landlord.builder()
                .type(LandlordType.OFFLINE).build();
        when(security.isPrivileged()).thenReturn(true);

        service.requireCanEditDetails(row); // no throw
    }

    // ── helpers ──────────────────────────────────────────────────────

    private static User userWithId(UUID id) {
        User u = User.builder().email("u@example.co.za")
                .firstName("U").surname("ser").build();
        try {
            Field f = BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return u;
    }

    private static <T> T withId(T entity, UUID id) {
        try {
            Field f = BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return entity;
    }
}

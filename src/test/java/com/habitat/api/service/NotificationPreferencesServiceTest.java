package com.habitat.api.service;

import com.habitat.api.dto.notification.NotificationPreferenceMatrix;
import com.habitat.api.dto.notification.NotificationPreferenceUpdate;
import com.habitat.api.entity.NotificationPreference;
import com.habitat.api.enums.NotificationCategory;
import com.habitat.api.enums.NotificationChannel;
import com.habitat.api.exception.ValidationException;
import com.habitat.api.repository.NotificationPreferenceRepository;
import com.habitat.api.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferencesServiceTest {

    @Mock NotificationPreferenceRepository repo;
    @Mock SecurityUtils security;

    @InjectMocks NotificationPreferencesService service;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void getMyMatrix_defaults_every_unstored_cell_to_enabled() {
        when(security.requireUserId()).thenReturn(USER_ID);
        when(repo.findByUserId(USER_ID)).thenReturn(List.of());

        NotificationPreferenceMatrix m = service.getMyMatrix();

        // Every cell should be enabled by default.
        long enabledCount = m.cells().stream().filter(NotificationPreferenceMatrix.Cell::enabled).count();
        assertThat(enabledCount).isEqualTo(m.cells().size());
        // 6 categories × 3 channels = 18 cells.
        assertThat(m.cells()).hasSize(18);
    }

    @Test
    void getMyMatrix_marks_SYSTEM_IN_APP_as_locked() {
        when(security.requireUserId()).thenReturn(USER_ID);
        when(repo.findByUserId(USER_ID)).thenReturn(List.of());

        NotificationPreferenceMatrix m = service.getMyMatrix();
        NotificationPreferenceMatrix.Cell locked = m.cells().stream()
                .filter(c -> c.category() == NotificationCategory.SYSTEM
                        && c.channel() == NotificationChannel.IN_APP)
                .findFirst()
                .orElseThrow();
        assertThat(locked.locked()).isTrue();
        assertThat(locked.enabled()).isTrue();
    }

    @Test
    void getMyMatrix_returns_stored_value_for_explicit_opt_outs() {
        when(security.requireUserId()).thenReturn(USER_ID);
        when(repo.findByUserId(USER_ID)).thenReturn(List.of(
                NotificationPreference.builder()
                        .userId(USER_ID)
                        .category(NotificationCategory.MARKETING)
                        .channel(NotificationChannel.EMAIL)
                        .enabled(false)
                        .build()));

        NotificationPreferenceMatrix m = service.getMyMatrix();
        NotificationPreferenceMatrix.Cell opted = m.cells().stream()
                .filter(c -> c.category() == NotificationCategory.MARKETING
                        && c.channel() == NotificationChannel.EMAIL)
                .findFirst()
                .orElseThrow();
        assertThat(opted.enabled()).isFalse();
        assertThat(opted.locked()).isFalse();
    }

    @Test
    void update_inserts_a_new_row_when_none_exists() {
        when(security.requireUserId()).thenReturn(USER_ID);
        when(repo.findByUserIdAndCategoryAndChannel(USER_ID,
                NotificationCategory.MARKETING, NotificationChannel.EMAIL))
                .thenReturn(Optional.empty());

        service.update(new NotificationPreferenceUpdate(
                NotificationCategory.MARKETING, NotificationChannel.EMAIL, false));

        verify(repo).save(any(NotificationPreference.class));
    }

    @Test
    void update_mutates_existing_row_when_one_exists() {
        when(security.requireUserId()).thenReturn(USER_ID);
        NotificationPreference existing = NotificationPreference.builder()
                .userId(USER_ID)
                .category(NotificationCategory.MARKETING)
                .channel(NotificationChannel.EMAIL)
                .enabled(false)
                .build();
        when(repo.findByUserIdAndCategoryAndChannel(USER_ID,
                NotificationCategory.MARKETING, NotificationChannel.EMAIL))
                .thenReturn(Optional.of(existing));

        service.update(new NotificationPreferenceUpdate(
                NotificationCategory.MARKETING, NotificationChannel.EMAIL, true));

        assertThat(existing.isEnabled()).isTrue();
        verify(repo, never()).save(any());
    }

    @Test
    void update_rejects_SYSTEM_IN_APP_disable_with_422() {
        when(security.requireUserId()).thenReturn(USER_ID);

        assertThatThrownBy(() -> service.update(new NotificationPreferenceUpdate(
                NotificationCategory.SYSTEM, NotificationChannel.IN_APP, false)))
                .isInstanceOf(ValidationException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void update_allows_SYSTEM_EMAIL_disable() {
        when(security.requireUserId()).thenReturn(USER_ID);
        when(repo.findByUserIdAndCategoryAndChannel(USER_ID,
                NotificationCategory.SYSTEM, NotificationChannel.EMAIL))
                .thenReturn(Optional.empty());

        service.update(new NotificationPreferenceUpdate(
                NotificationCategory.SYSTEM, NotificationChannel.EMAIL, false));

        verify(repo).save(any(NotificationPreference.class));
    }

    @Test
    void isOptedIn_returns_true_for_locked_SYSTEM_IN_APP_even_if_repo_says_false() {
        // The locked check short-circuits before the repo is consulted at all.
        boolean opted = service.isOptedIn(USER_ID, NotificationCategory.SYSTEM, NotificationChannel.IN_APP);
        assertThat(opted).isTrue();
    }

    @Test
    void isOptedIn_returns_stored_value_for_unlocked_cells() {
        when(repo.findByUserIdAndCategoryAndChannel(USER_ID,
                NotificationCategory.MARKETING, NotificationChannel.EMAIL))
                .thenReturn(Optional.of(NotificationPreference.builder()
                        .userId(USER_ID)
                        .category(NotificationCategory.MARKETING)
                        .channel(NotificationChannel.EMAIL)
                        .enabled(false)
                        .build()));

        boolean opted = service.isOptedIn(USER_ID,
                NotificationCategory.MARKETING, NotificationChannel.EMAIL);
        assertThat(opted).isFalse();
    }

    @Test
    void isOptedIn_defaults_to_true_when_no_row() {
        when(repo.findByUserIdAndCategoryAndChannel(USER_ID,
                NotificationCategory.BILLING, NotificationChannel.SMS))
                .thenReturn(Optional.empty());

        boolean opted = service.isOptedIn(USER_ID,
                NotificationCategory.BILLING, NotificationChannel.SMS);
        assertThat(opted).isTrue();
    }
}

package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.user.UserMeResponse;
import com.habitat.api.entity.User;
import com.habitat.api.enums.Role;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void getMe_returns_dto_for_known_user() {
        User user = userOf(USER_ID, "a@example.co.za", Role.USER, Role.USER);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserMeResponse out = userService.getMe(USER_ID);

        assertThat(out.id()).isEqualTo(USER_ID);
        assertThat(out.email()).isEqualTo("a@example.co.za");
        assertThat(out.activeRole()).isEqualTo(Role.USER);
        assertThat(out.roles()).containsExactly(Role.USER);
    }

    @Test
    void getMe_throws_when_user_missing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getMe(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ErrorMessages.USER_NOT_FOUND);
    }

    @Test
    void switchActiveRole_persists_and_returns_updated() {
        User user = userOf(USER_ID, "a@example.co.za", Role.USER, Role.USER, Role.AGENT);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserMeResponse out = userService.switchActiveRole(USER_ID, Role.AGENT);

        assertThat(out.activeRole()).isEqualTo(Role.AGENT);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getActiveRole()).isEqualTo(Role.AGENT);
    }

    @Test
    void switchActiveRole_is_idempotent_when_role_unchanged() {
        User user = userOf(USER_ID, "a@example.co.za", Role.USER, Role.USER);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserMeResponse out = userService.switchActiveRole(USER_ID, Role.USER);

        assertThat(out.activeRole()).isEqualTo(Role.USER);
        verify(userRepository, never()).save(user);
    }

    @Test
    void switchActiveRole_throws_when_role_not_owned() {
        User user = userOf(USER_ID, "a@example.co.za", Role.USER, Role.USER);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.switchActiveRole(USER_ID, Role.ADMIN))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage(ErrorMessages.ROLE_NOT_OWNED);
        verify(userRepository, never()).save(user);
    }

    @Test
    void switchActiveRole_throws_when_user_missing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.switchActiveRole(USER_ID, Role.USER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static User userOf(UUID id, String email, Role active, Role... allRoles) {
        Set<Role> roles = new HashSet<>(Set.of(allRoles));
        User u = User.builder()
                .email(email)
                .firstName("Test")
                .surname("User")
                .passwordHash("h")
                .roles(roles)
                .activeRole(active)
                .emailVerified(true)
                .build();
        try {
            var f = com.habitat.api.entity.base.BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return u;
    }
}

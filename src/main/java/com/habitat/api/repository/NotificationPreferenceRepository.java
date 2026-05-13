package com.habitat.api.repository;

import com.habitat.api.entity.NotificationPreference;
import com.habitat.api.entity.NotificationPreference.PreferenceKey;
import com.habitat.api.enums.NotificationCategory;
import com.habitat.api.enums.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository
        extends JpaRepository<NotificationPreference, PreferenceKey> {

    List<NotificationPreference> findByUserId(UUID userId);

    Optional<NotificationPreference> findByUserIdAndCategoryAndChannel(
            UUID userId, NotificationCategory category, NotificationChannel channel);
}

package com.habitat.api.repository;

import com.habitat.api.entity.User;
import com.habitat.api.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Total users whose currently active role matches — drives the
     * landing-stats "registered tenants" count. Counts the active role
     * (single value) rather than the multi-valued {@code roles} set so
     * agents who also hold USER aren't double-counted toward the tenant
     * pool. {@link Role#USER} covers both prospective tenants and
     * landlord-only accounts; we surface it under "Registered tenants"
     * because the catalogue isn't big enough yet for finer-grained
     * distinction.
     */
    long countByActiveRole(Role activeRole);
}

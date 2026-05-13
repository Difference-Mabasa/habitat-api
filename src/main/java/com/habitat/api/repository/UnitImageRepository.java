package com.habitat.api.repository;

import com.habitat.api.entity.UnitImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UnitImageRepository extends JpaRepository<UnitImage, UUID> {
}

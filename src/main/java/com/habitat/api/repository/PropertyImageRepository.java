package com.habitat.api.repository;

import com.habitat.api.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, UUID> {
}

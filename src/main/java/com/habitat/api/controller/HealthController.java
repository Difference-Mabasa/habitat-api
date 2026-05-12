package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping(ApiRoutes.HEALTH)
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "habitat-api",
                "timestamp", OffsetDateTime.now()
        );
    }
}

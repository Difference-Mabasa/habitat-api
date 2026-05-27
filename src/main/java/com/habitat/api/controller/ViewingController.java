package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.viewing.RequestViewingRequest;
import com.habitat.api.dto.viewing.ReviewViewingRequest;
import com.habitat.api.dto.viewing.ViewingResponse;
import com.habitat.api.service.ViewingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tenant viewing endpoints.
 *
 * <ul>
 *   <li>POST /viewings — tenant books a slot</li>
 *   <li>GET /viewings/mine — tenant's own viewings</li>
 *   <li>GET /viewings/managed — viewings against properties the
 *       caller manages</li>
 *   <li>POST /viewings/{id}/approve — manager approves</li>
 *   <li>POST /viewings/{id}/reject — manager rejects</li>
 *   <li>POST /viewings/{id}/cancel — either party cancels</li>
 * </ul>
 */
@RestController
@RequestMapping(ApiRoutes.VIEWINGS)
@RequiredArgsConstructor
public class ViewingController {

    private final ViewingService viewings;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ViewingResponse request(@Valid @RequestBody RequestViewingRequest req) {
        return viewings.request(req);
    }

    @GetMapping("/mine")
    public List<ViewingResponse> listMine() {
        return viewings.listMine();
    }

    @GetMapping("/managed")
    public List<ViewingResponse> listManaged() {
        return viewings.listManaged();
    }

    @PostMapping("/{id}/approve")
    public ViewingResponse approve(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ReviewViewingRequest req
    ) {
        return viewings.approve(id, req);
    }

    @PostMapping("/{id}/reject")
    public ViewingResponse reject(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ReviewViewingRequest req
    ) {
        return viewings.reject(id, req);
    }

    @PostMapping("/{id}/cancel")
    public ViewingResponse cancel(@PathVariable UUID id) {
        return viewings.cancel(id);
    }
}

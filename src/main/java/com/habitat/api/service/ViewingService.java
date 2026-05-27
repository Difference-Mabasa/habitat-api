package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.viewing.RequestViewingRequest;
import com.habitat.api.dto.viewing.ReviewViewingRequest;
import com.habitat.api.dto.viewing.ViewingResponse;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.User;
import com.habitat.api.entity.Viewing;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.enums.ViewingStatus;
import com.habitat.api.event.ViewingApprovedEvent;
import com.habitat.api.event.ViewingCancelledEvent;
import com.habitat.api.event.ViewingRejectedEvent;
import com.habitat.api.event.ViewingRequestedEvent;
import com.habitat.api.exception.BadRequestException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.UnitRepository;
import com.habitat.api.repository.UserRepository;
import com.habitat.api.repository.ViewingRepository;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.service.statemachine.ViewingStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Tenant viewing lifecycle: request → approve | reject → cancel.
 * Every transition publishes a typed event consumed by an
 * AFTER_COMMIT listener so a comms failure can't roll back the
 * state change.
 *
 * <p>Authorization:
 * <ul>
 *   <li>Request — any authenticated user; unit must be AVAILABLE.</li>
 *   <li>Approve / reject — caller must pass
 *       {@link PropertyService#canEdit} on the unit's property.</li>
 *   <li>Cancel — either the tenant who requested OR a manager of
 *       the property.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ViewingService {

    private final ViewingRepository viewings;
    private final UnitRepository units;
    private final UserRepository users;
    private final PropertyService propertyService;
    private final SecurityUtils security;
    private final ApplicationEventPublisher events;

    // ── Tenant: book a viewing ───────────────────────────────────────

    @Transactional
    public ViewingResponse request(RequestViewingRequest req) {
        UUID me = security.requireUserId();
        User tenant = users.findById(me)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));
        Unit unit = units.findById(req.unitId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.UNIT_NOT_FOUND));
        if (unit.getStatus() != UnitStatus.AVAILABLE) {
            throw new BadRequestException(ErrorMessages.VIEWING_UNIT_NOT_AVAILABLE);
        }
        if (req.scheduledAt().isBefore(OffsetDateTime.now())) {
            throw new BadRequestException(ErrorMessages.VIEWING_SCHEDULED_AT_PAST);
        }

        Viewing viewing = Viewing.builder()
                .unit(unit)
                .tenant(tenant)
                .scheduledAt(req.scheduledAt())
                .status(ViewingStatus.REQUESTED)
                .notes(blankToNull(req.notes()))
                .build();
        Viewing saved = viewings.save(viewing);
        log.info("viewing {} requested by tenant {} for unit {}",
                saved.getId(), me, unit.getId());
        events.publishEvent(new ViewingRequestedEvent(saved.getId()));
        return ViewingResponse.from(saved);
    }

    // ── Manager: approve / reject ────────────────────────────────────

    @Transactional
    public ViewingResponse approve(UUID viewingId, ReviewViewingRequest req) {
        Viewing v = requireManagerOnViewing(viewingId);
        ViewingStateMachine.transition(v, ViewingStatus.APPROVED);
        applyDecision(v, req);
        log.info("viewing {} approved by {}", v.getId(), security.requireUserId());
        events.publishEvent(new ViewingApprovedEvent(v.getId()));
        return ViewingResponse.from(v);
    }

    @Transactional
    public ViewingResponse reject(UUID viewingId, ReviewViewingRequest req) {
        Viewing v = requireManagerOnViewing(viewingId);
        ViewingStateMachine.transition(v, ViewingStatus.REJECTED);
        applyDecision(v, req);
        log.info("viewing {} rejected by {}", v.getId(), security.requireUserId());
        events.publishEvent(new ViewingRejectedEvent(v.getId()));
        return ViewingResponse.from(v);
    }

    // ── Either party: cancel ─────────────────────────────────────────

    @Transactional
    public ViewingResponse cancel(UUID viewingId) {
        UUID me = security.requireUserId();
        Viewing v = viewings.findById(viewingId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.VIEWING_NOT_FOUND));
        boolean isTenant = v.getTenant() != null && me.equals(v.getTenant().getId());
        boolean isManager = propertyService.canEdit(v.getUnit().getProperty());
        if (!isTenant && !isManager) {
            throw new ForbiddenException(ErrorMessages.FORBIDDEN);
        }
        ViewingStateMachine.transition(v, ViewingStatus.CANCELLED);
        v.setCancelledBy(me);
        log.info("viewing {} cancelled by {} (tenant={}, manager={})",
                v.getId(), me, isTenant, isManager);
        events.publishEvent(new ViewingCancelledEvent(v.getId()));
        return ViewingResponse.from(v);
    }

    // ── Lists ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ViewingResponse> listMine() {
        UUID me = security.requireUserId();
        return viewings.findByTenant_IdOrderByScheduledAtDesc(me).stream()
                .map(ViewingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ViewingResponse> listManaged() {
        UUID me = security.requireUserId();
        return viewings.findByUnit_Property_Manager_IdOrderByScheduledAtAsc(me).stream()
                .map(ViewingResponse::from)
                .toList();
    }

    // ── helpers ──────────────────────────────────────────────────────

    private Viewing requireManagerOnViewing(UUID viewingId) {
        Viewing v = viewings.findById(viewingId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.VIEWING_NOT_FOUND));
        propertyService.requireCanEdit(v.getUnit().getProperty());
        return v;
    }

    private void applyDecision(Viewing v, ReviewViewingRequest req) {
        UUID me = security.requireUserId();
        v.setDecisionNote(blankToNull(req == null ? null : req.decisionNote()));
        v.setDecidedAt(OffsetDateTime.now());
        v.setDecidedBy(me);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

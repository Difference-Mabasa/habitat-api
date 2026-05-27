package com.habitat.api.service.statemachine;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.entity.Viewing;
import com.habitat.api.enums.ViewingStatus;
import com.habitat.api.exception.ConflictException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Legal {@link ViewingStatus} transitions. Mirrors
 * {@code ApplicationStateMachine}'s shape — keep the state-write
 * discipline in one place per entity.
 */
public final class ViewingStateMachine {

    private static final Map<ViewingStatus, Set<ViewingStatus>> LEGAL = Map.of(
            ViewingStatus.REQUESTED, EnumSet.of(
                    ViewingStatus.APPROVED,
                    ViewingStatus.REJECTED,
                    ViewingStatus.CANCELLED),
            ViewingStatus.APPROVED, EnumSet.of(
                    ViewingStatus.CANCELLED,
                    ViewingStatus.COMPLETED),
            // Terminal — no outgoing transitions.
            ViewingStatus.REJECTED,  EnumSet.noneOf(ViewingStatus.class),
            ViewingStatus.CANCELLED, EnumSet.noneOf(ViewingStatus.class),
            ViewingStatus.COMPLETED, EnumSet.noneOf(ViewingStatus.class)
    );

    private ViewingStateMachine() {}

    public static boolean canTransition(ViewingStatus current, ViewingStatus next) {
        Set<ViewingStatus> allowed = LEGAL.get(current);
        return allowed != null && allowed.contains(next);
    }

    public static void transition(Viewing viewing, ViewingStatus next) {
        ViewingStatus current = viewing.getStatus();
        if (current == next) return;
        if (!canTransition(current, next)) {
            throw new ConflictException(ErrorMessages.VIEWING_INVALID_TRANSITION);
        }
        viewing.setStatus(next);
    }
}

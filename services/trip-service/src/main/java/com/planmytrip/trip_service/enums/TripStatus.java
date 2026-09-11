package com.planmytrip.trip_service.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TripStatus {

    DRAFT,
    PLAN_READY,
    BOOKED,
    COMPLETED,
    CANCELLED;

    /**
     * Returns the only status this one is allowed to move to next in the standard progression,
     * or null if this status is terminal.
     */
    public TripStatus next() {
        return switch (this) {
            case DRAFT -> PLAN_READY;
            case PLAN_READY -> BOOKED;
            case BOOKED -> COMPLETED;
            case COMPLETED, CANCELLED -> null;
        };
    }

    public boolean canTransitionTo(TripStatus target) {
        if (target == null) {
            return false;
        }
        if (target == CANCELLED) {
            return this != COMPLETED && this != CANCELLED;
        }
        return target == this.next();
    }

    @JsonCreator
    public static TripStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return TripStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown status: '" + value + "'. Allowed values: DRAFT, PLAN_READY, BOOKED, COMPLETED, CANCELLED");
        }
    }
}

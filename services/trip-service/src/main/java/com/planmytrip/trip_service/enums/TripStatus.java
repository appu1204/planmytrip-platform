package com.planmytrip.trip_service.enums;

public enum TripStatus {

    DRAFT,
    PLAN_READY,
    BOOKED,
    COMPLETED;


    /**
     * Returns the only status this one is allowed to move to next, or null
     * if this status is terminal.
     */
    public TripStatus next() {
        return switch (this) {
            case DRAFT -> PLAN_READY;
            case PLAN_READY -> BOOKED;
            case BOOKED -> COMPLETED;
            case COMPLETED -> null;
        };
    }

    public boolean canTransitionTo(TripStatus target) {
        return target != null && target == this.next();
    }
    
}

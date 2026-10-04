package com.planmytrip.trip_service.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TripType {

    SOLO,
    COUPLE,
    FRIENDS,
    FAMILY,
    ADVENTURE;

    @JsonCreator
    public static TripType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return TripType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Graceful fallback for unrecognized persona names
            return null;
        }
    }
}

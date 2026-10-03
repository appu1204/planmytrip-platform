package com.planmytrip.trip_service.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TripType {

    SOLO,
    COUPLE,
    FRIENDS,
    FAMILY;

    @JsonCreator
    public static TripType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return TripType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown trip type: '" + value + "'. Allowed values: SOLO, COUPLE, FRIENDS, FAMILY");
        }
    }
}


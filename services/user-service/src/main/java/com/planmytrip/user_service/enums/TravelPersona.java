package com.planmytrip.user_service.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TravelPersona {
    SOLO,
    COUPLE,
    FRIENDS,
    FAMILY;

    @JsonCreator
    public static TravelPersona from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return TravelPersona.valueOf(value.trim().toUpperCase());
    }
}


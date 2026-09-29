package com.planmytrip.booking_service.dto.response;

public record ProviderBookingResult(
        String providerBookingId,
        boolean success,
        String errorCode
) {}

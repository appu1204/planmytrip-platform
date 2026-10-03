package com.planmytrip.trip_service.service;

import java.util.UUID;

import com.planmytrip.trip_service.dto.request.UpdateTripPreferencesRequest;
import com.planmytrip.trip_service.dto.response.TripPreferencesResponse;

public interface TripPreferenceService {

    TripPreferencesResponse setPreferences(UUID tripId, UpdateTripPreferencesRequest request);
    TripPreferencesResponse setPreferences(UUID tripId, UpdateTripPreferencesRequest request, Long userId);
    TripPreferencesResponse getPreferences(UUID tripId);
    TripPreferencesResponse getPreferences(UUID tripId, Long userId);
}

package com.planmytrip.trip_service.controller;

import com.planmytrip.trip_service.dto.request.UpdateTripPreferencesRequest;
import com.planmytrip.trip_service.dto.response.TripPreferencesResponse;
import com.planmytrip.trip_service.service.TripPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/trips/{tripId}/preferences")
@RequiredArgsConstructor
@Tag(name = "Trip Preferences", description = "What the traveller enjoys, for a specific trip")
public class TripPreferenceController {

    private final TripPreferenceService tripPreferenceService;

    @PutMapping
    @Operation(summary = "Set trip preferences",
            description = "Replaces the full preference set for this trip. " +
                    "Blocked once the trip is BOOKED or COMPLETED.")
    public ResponseEntity<TripPreferencesResponse> setPreferences(
            @PathVariable UUID tripId,
            @Valid @RequestBody UpdateTripPreferencesRequest request) {
        return ResponseEntity.ok(tripPreferenceService.setPreferences(tripId, request));
    }

    @GetMapping
    @Operation(summary = "Get trip preferences")
    public ResponseEntity<TripPreferencesResponse> getPreferences(@PathVariable UUID tripId) {
        return ResponseEntity.ok(tripPreferenceService.getPreferences(tripId));
    }
}

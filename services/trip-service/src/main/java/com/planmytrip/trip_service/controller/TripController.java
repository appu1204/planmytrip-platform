package com.planmytrip.trip_service.controller;

import com.planmytrip.trip_service.dto.request.CreateTripRequest;
import com.planmytrip.trip_service.dto.request.UpdateTripRequest;
import com.planmytrip.trip_service.dto.request.UpdateTripStatusRequest;
import com.planmytrip.trip_service.dto.response.PageResponse;
import com.planmytrip.trip_service.dto.response.TripResponse;
import com.planmytrip.trip_service.enums.TripStatus;
import com.planmytrip.trip_service.service.TripService;
import com.planmytrip.trip_service.exception.InvalidTripRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
// import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
@Tag(name = "Trips", description = "Trip lifecycle management")
public class TripController {

    private final TripService tripService;

    @PostMapping
    @Operation(summary = "Start a new trip", description = "Creates a trip in DRAFT status")
    public ResponseEntity<TripResponse> createTrip(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Valid @RequestBody CreateTripRequest request) {
        if (request.getUserId() == null) {
            if (headerUserId != null) {
                request.setUserId(headerUserId);
            } else {
                throw new InvalidTripRequestException("userId is required");
            }
        }
        TripResponse response = tripService.createTrip(request);
        return ResponseEntity
                .created(URI.create("/api/trip/trips/" + response.getId()))
                .body(response);
    }

    @GetMapping
    @Operation(summary = "List a user's trips", description = "Optionally filter by status. Sorted newest-first.")
    public ResponseEntity<PageResponse<TripResponse>> listTrips(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) TripStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long effectiveUserId = (userId != null) ? userId : headerUserId;
        if (effectiveUserId == null) {
            throw new InvalidTripRequestException("userId is required");
        }
        return ResponseEntity.ok(tripService.listTrips(effectiveUserId, status, page, size));
    }

    @GetMapping("/{tripId}")
    @Operation(summary = "Get a single trip by id")
    public ResponseEntity<TripResponse> getTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(tripService.getTripById(tripId));
    }

    @PatchMapping("/{tripId}")
    @Operation(summary = "Edit trip details",
            description = "Partial update — only send the fields you want to change. " +
                    "Blocked once the trip is BOOKED or COMPLETED.")

    public ResponseEntity<TripResponse> updateTrip(
            @PathVariable UUID tripId,
            @Valid @RequestBody UpdateTripRequest request) {

        return ResponseEntity.ok(tripService.updateTrip(tripId, request));
            }

    @DeleteMapping("/{tripId}")
    @Operation(summary = "Delete a trip",
            description = "Blocked once the trip is BOOKED or COMPLETED.")
    public ResponseEntity<Void> deleteTrip(@PathVariable UUID tripId) {
        tripService.deleteTrip(tripId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{tripId}/status")
    @Operation(summary = "Move a trip to its next status",
            description = "Forward-only, one step at a time: DRAFT -> PLAN_READY -> BOOKED -> COMPLETED. " +
                    "Skipping a step or going backwards is rejected.")
    public ResponseEntity<TripResponse> updateTripStatus(
            @PathVariable UUID tripId,
            @Valid @RequestBody UpdateTripStatusRequest request) {
        return ResponseEntity.ok(tripService.updateTripStatus(tripId, request.getStatus()));
    }

}

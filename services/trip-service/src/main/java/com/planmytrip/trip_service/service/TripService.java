package com.planmytrip.trip_service.service;

import com.planmytrip.trip_service.dto.response.PageResponse;
import com.planmytrip.trip_service.dto.response.TripResponse;
import com.planmytrip.trip_service.enums.TripStatus;
import com.planmytrip.trip_service.dto.request.CreateTripRequest;
import com.planmytrip.trip_service.dto.request.UpdateTripRequest;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service

public interface TripService {

    /**
     * Backlog story: "Let user start a new trip".
     * Creates a trip in DRAFT status.
     */
    TripResponse createTrip(CreateTripRequest request);

    /**
     * Backlog story: "Let user see all their trips".
     * status is optional — null means "all statuses".
     */
    PageResponse<TripResponse> listTrips(Long userId, TripStatus status, int page, int size);

    /**
     * Fetch a single trip by id. 404 via TripNotFoundException if missing.
     */
    TripResponse getTripById(UUID tripId);

    /**
     * Backlog story: "Let user edit trip details".
     * Partial update — only non-null fields in the request are applied.
     * Blocked once the trip is BOOKED or COMPLETED.
     */
    TripResponse updateTrip(UUID tripId, UpdateTripRequest request);

    /**
     * Backlog story: "Let user delete a trip".
     * Blocked once the trip is BOOKED or COMPLETED — same rule as editing.
     * Preferences for the trip cascade-delete at the DB level (FK ON DELETE CASCADE).
     */
    void deleteTrip(UUID tripId);

    /**
     * Backlog story: "Show trip status clearly".
     * Enforces the forward-only DRAFT -> PLAN_READY -> BOOKED -> COMPLETED
     * progression — see TripStatus.canTransitionTo for the exact rule.
     */
    TripResponse updateTripStatus(UUID tripId, TripStatus newStatus);

}

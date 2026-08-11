package com.planmytrip.trip_service.service;

import com.planmytrip.trip_service.dto.response.TripResponse;
import com.planmytrip.trip_service.dto.request.CreateTripRequest;
import org.springframework.stereotype.Service;

@Service

public interface TripService {

    /**
     * Backlog story: "Let user start a new trip".
     * Creates a trip in DRAFT status.
     */
    TripResponse createTrip(CreateTripRequest request);

}

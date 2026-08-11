package com.planmytrip.trip_service.controller;

import com.planmytrip.trip_service.dto.request.CreateTripRequest;
import com.planmytrip.trip_service.dto.response.TripResponse;
import com.planmytrip.trip_service.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@Tag(name = "Trips", description = "Trip lifecycle management")
public class TripController {

    private final TripService tripService;

    @PostMapping
    @Operation(summary = "Start a new trip", description = "Creates a trip in DRAFT status")
    public ResponseEntity<TripResponse> createTrip(@Valid @RequestBody CreateTripRequest request) {
        TripResponse response = tripService.createTrip(request);
        return ResponseEntity
                .created(URI.create("/api/trips/" + response.getId()))
                .body(response);
    }
}

package com.planmytrip.trip_service.controller;

import com.planmytrip.trip_service.dto.response.TripResponse;
import com.planmytrip.trip_service.dto.response.WeatherAdvisoryResponse;
import com.planmytrip.trip_service.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
@Tag(name = "Weather Advisory", description = "Pre-trip weather and natural hazard environmental advisory service")
public class WeatherAdvisoryController {

    private final TripService tripService;
    private final com.planmytrip.trip_service.service.WeatherAdvisoryService weatherAdvisoryService;

    @GetMapping("/{tripId}/weather-advisory")
    @Operation(summary = "Get weather safety advisory for a trip",
            description = "Evaluates destination atmospheric conditions and natural hazard risks (heavy rain, wind gusts, landslides) for the trip")
    public ResponseEntity<WeatherAdvisoryResponse> getTripWeatherAdvisory(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @PathVariable UUID tripId) {
        TripResponse trip = tripService.getTripById(tripId, headerUserId);
        String destination = (trip.getDestination() != null && !trip.getDestination().isBlank())
                ? trip.getDestination()
                : "your destination";
        return ResponseEntity.ok(weatherAdvisoryService.getAdvisory(destination));
    }

    @GetMapping("/weather/check")
    @Operation(summary = "Check weather safety by destination name",
            description = "On-demand natural hazard clearance check for immediate or upcoming tours")
    public ResponseEntity<WeatherAdvisoryResponse> checkWeatherSafety(
            @RequestParam(defaultValue = "your destination") String destination) {
        return ResponseEntity.ok(weatherAdvisoryService.getAdvisory(destination));
    }
}

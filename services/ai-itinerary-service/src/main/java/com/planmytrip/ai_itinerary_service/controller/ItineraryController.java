package com.planmytrip.ai_itinerary_service.controller;

import com.planmytrip.ai_itinerary_service.dto.request.GenerateItineraryRequest;
import com.planmytrip.ai_itinerary_service.dto.response.ItineraryResponse;
import com.planmytrip.ai_itinerary_service.dto.response.ItinerarySummaryResponse;
import com.planmytrip.ai_itinerary_service.service.ItineraryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
// @RequestMapping("/api/v1/itineraries")
@RequiredArgsConstructor
@Tag(
        name = "AI Itinerary Service",
        description = "Generate, save, view, regenerate and cost-break-down AI trip plans"
)
public class ItineraryController {

    private final ItineraryService itineraryService;

    @PostMapping("/generate")
    @Operation(
            summary = "Generate a day-by-day AI itinerary for a trip (creates a DRAFT, version 1)"
    )
    public ResponseEntity<ItineraryResponse> generate(
            @Valid @RequestBody GenerateItineraryRequest request
    ) {
        ItineraryResponse response =
                itineraryService.generate(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation (summary = "Confirm/save a generated draft as the trip's active itinerary")
    @PatchMapping ("/{itineraryId}/save")
    public ResponseEntity<ItineraryResponse> save(@PathVariable UUID itineraryID) {
        return ResponseEntity.ok(itineraryService.save(itineraryID));
    }

    @Operation(summary = "view one full itinerary (all days + activities + budget)")
    @GetMapping ("/{itineraryId}")
    public ResponseEntity<ItineraryResponse> getById(@PathVariable UUID itineraryID) {
        return ResponseEntity.ok(itineraryService.getById(itineraryID));
    }

    @Operation(summary = "List every generated version of a trip (lightweight summaries)")
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<ItinerarySummaryResponse>> getHistoryForTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(itineraryService.getHistoryForTrip(tripId));
    }

    @Operation (summary = "Get the trip's currently saved/active itinerary")
    @GetMapping("trip/{tripId}/active")
    public ResponseEntity<ItineraryResponse> getActiveForTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(itineraryService.getActiveForTrip(tripId));
    }
}
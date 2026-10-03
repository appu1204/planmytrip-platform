package com.planmytrip.ai_itinerary_service.controller;

import com.planmytrip.ai_itinerary_service.dto.request.GenerateItineraryRequest;
import com.planmytrip.ai_itinerary_service.dto.response.BudgetResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    // phase 1:
    /**
     * Generate day-by-day plan for a trip
     * POST /api/v1/ai-itinerary/generate
     */

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

    // phase 2:
    /**
     * Confirm/save a generated draft as the trip's active itinerary
     * Save the generated plan as the trip's active itinerary
     */

    @Operation (summary = "Confirm/save a generated draft as the trip's active itinerary")
    @PatchMapping ("/{itineraryId}/save")
    public ResponseEntity<ItineraryResponse> save(@PathVariable("itineraryId") UUID itineraryId) {
        return ResponseEntity.ok(itineraryService.save(itineraryId));
    }


    // it was to feature phase 3 but i decided to add it to phase 2 becuase phase 2 was simple feature and it took less so implemeted in phase 2 only
    /**
     * View full plan for a trip
     * GET /api/v1/ai-itinerary/{id}
     */

    @Operation(summary = "view one full itinerary (all days + activities + budget)")
    @GetMapping ("/{itineraryId}")
    public ResponseEntity<ItineraryResponse> getById(@PathVariable("itineraryId") UUID itineraryId) {
        return ResponseEntity.ok(itineraryService.getById(itineraryId));
    }

    /**
     * List every generated version of a trip (lightweight summaries)
     * GET /api/v1/ai-itinerary/trip/{tripId}
     */

    @Operation(summary = "List every generated version of a trip (lightweight summaries)")
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<ItinerarySummaryResponse>> getHistoryForTrip(@PathVariable("tripId") UUID tripId) {
        return ResponseEntity.ok(itineraryService.getHistoryForTrip(tripId));
    }

    /**
     * Get the trip's currently saved/active itinerary
     * GET /api/v1/ai-itinerary/trip/{tripId}/active
     */

    @Operation (summary = "Get the trip's currently saved/active itinerary")
    @GetMapping({"/trip/{tripId}/active", "/trip/{tripId}/itinerary", "/trips/{tripId}/itinerary"})
    public ResponseEntity<ItineraryResponse> getActiveForTrip(@PathVariable("tripId") UUID tripId) {
        return ResponseEntity.ok(itineraryService.getActiveForTrip(tripId));
    }

    @Operation(summary = "Save or update a trip's itinerary")
    @PutMapping({"/trip/{tripId}/itinerary", "/trips/{tripId}/itinerary", "/trip/{tripId}", "/trips/{tripId}"})
    public ResponseEntity<ItineraryResponse> saveItineraryForTrip(
            @PathVariable("tripId") UUID tripId,
            @RequestBody(required = false) java.util.Map<String, Object> payload) {
        return ResponseEntity.ok(itineraryService.saveItineraryForTrip(tripId, payload));
    }

    @Operation(summary = "Generate an AI itinerary scoped to an existing trip")
    @PostMapping({"/trips/{tripId}/itinerary/generate", "/trip/{tripId}/itinerary/generate", "/trip/{tripId}/generate"})
    public ResponseEntity<ItineraryResponse> generateForTrip(
            @PathVariable("tripId") UUID tripId,
            @RequestBody(required = false) GenerateItineraryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itineraryService.generateForTrip(tripId, request));
    }

    @Operation(summary = "Regenerate an AI itinerary scoped to an existing trip")
    @PostMapping({"/trips/{tripId}/itinerary/regenerate", "/trip/{tripId}/itinerary/regenerate", "/trip/{tripId}/regenerate"})
    public ResponseEntity<ItineraryResponse> regenerateForTrip(
            @PathVariable("tripId") UUID tripId,
            @RequestBody(required = false) GenerateItineraryRequest overrides) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itineraryService.regenerateForTrip(tripId, overrides));
    }

    @Operation(summary = "Get budget breakdown for a trip")
    @GetMapping({"/trips/{tripId}/budget", "/trip/{tripId}/budget"})
    public ResponseEntity<BudgetResponse> getBudgetForTrip(@PathVariable("tripId") UUID tripId) {
        return ResponseEntity.ok(itineraryService.getBudgetForTrip(tripId));
    }

    @Operation(summary = "Save notes for a trip")
    @PatchMapping({"/trips/{tripId}/notes", "/trip/{tripId}/notes"})
    public ResponseEntity<?> saveNotes(
            @PathVariable("tripId") UUID tripId,
            @RequestBody(required = false) java.util.Map<String, Object> payload) {
        String notes = (payload != null && payload.get("notes") != null) ? payload.get("notes").toString() : "";
        return ResponseEntity.ok(itineraryService.saveNotes(tripId, notes));
    }

    @Operation(summary = "Get map route stops for a trip")
    @GetMapping({"/trips/{tripId}/route", "/trip/{tripId}/route"})
    public ResponseEntity<?> getRoute(@PathVariable("tripId") UUID tripId) {
        return ResponseEntity.ok(itineraryService.getRoute(tripId));
    }

    @Operation(summary = "Optimize route stops for a trip")
    @PostMapping({"/trips/{tripId}/route/optimize", "/trip/{tripId}/route/optimize"})
    public ResponseEntity<?> optimizeRoute(@PathVariable("tripId") UUID tripId) {
        return ResponseEntity.ok(itineraryService.getRoute(tripId));
    }

    @Operation(summary = "Add a day to an itinerary")
    @PostMapping({"/trips/{tripId}/itinerary/days", "/trip/{tripId}/itinerary/days"})
    public ResponseEntity<ItineraryResponse> addDay(
            @PathVariable("tripId") UUID tripId,
            @RequestBody java.util.Map<String, Object> day) {
        return ResponseEntity.ok(itineraryService.addDay(tripId, day));
    }

    @Operation(summary = "Add an activity to an itinerary day")
    @PostMapping({"/trips/{tripId}/itinerary/days/{dayId}/activities", "/trip/{tripId}/itinerary/days/{dayId}/activities"})
    public ResponseEntity<ItineraryResponse> addActivity(
            @PathVariable("tripId") UUID tripId,
            @PathVariable("dayId") String dayId,
            @RequestBody java.util.Map<String, Object> activity) {
        return ResponseEntity.ok(itineraryService.addActivity(tripId, dayId, activity));
    }

    @Operation(summary = "Delete an activity from an itinerary day")
    @DeleteMapping({"/trips/{tripId}/itinerary/days/{dayId}/activities/{activityId}", "/trip/{tripId}/itinerary/days/{dayId}/activities/{activityId}"})
    public ResponseEntity<ItineraryResponse> deleteActivity(
            @PathVariable("tripId") UUID tripId,
            @PathVariable("dayId") String dayId,
            @PathVariable("activityId") String activityId) {
        return ResponseEntity.ok(itineraryService.deleteActivity(tripId, dayId, activityId));
    }

    // phase 3:
    /**
     * Regenerate without losing old version 
     * POST /api/v1/ai-itinerary/{id}/regenerate
     */

    @Operation (summary = "Regenerate the plan as a new version, keeping older versions intact")
    @PostMapping ("/{itineraryId}/regenerate")
    public ResponseEntity<ItineraryResponse> regenerate(@PathVariable("itineraryId") UUID itineraryId, @RequestBody(required = false) GenerateItineraryRequest overrides) {
        ItineraryResponse response = itineraryService.regenerate(itineraryId, overrides);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // phase 4:
    /**
     * Budget breakdown
     * GET /api/v1/ai-itinerary/{id}/budget
     */

    @Operation (summary = "Get just the estimated cost breakdown for an itinerary")
    @GetMapping ("/{itineraryId}/budget")
    public ResponseEntity<BudgetResponse> getBudget(@PathVariable("itineraryId") UUID itineraryId) {
        return ResponseEntity.ok(itineraryService.getBudgetBreakdown(itineraryId));
    }

}
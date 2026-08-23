package com.planmytrip.ai_itinerary_service.controller;

import com.planmytrip.ai_itinerary_service.dto.request.GenerateItineraryRequest;
import com.planmytrip.ai_itinerary_service.dto.response.ItineraryResponse;
import com.planmytrip.ai_itinerary_service.service.ItineraryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/itineraries")
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
}
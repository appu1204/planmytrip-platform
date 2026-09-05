package com.planmytrip.trip_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.planmytrip.trip_service.enums.TripType;
import com.planmytrip.trip_service.dto.response.DestinationResponse;
import com.planmytrip.trip_service.service.DestinationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;



@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Destinations", description = "Curated destination suggestions for the home page")
public class DestinationController {


    private final DestinationService destinationService;

    @GetMapping("/popular")
    @Operation(summary = "Get popular destinations",
            description = "Powers the home page 'Popular Destinations' strip. " +
                    "Optionally filter by persona (SOLO, COUPLE, FRIENDS, FAMILY).")
    
    public ResponseEntity<List<DestinationResponse>> getPopularDestinations(
            @RequestParam(required = false) TripType persona,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(destinationService.getPopularDestinations(persona, limit));
    }
    
}

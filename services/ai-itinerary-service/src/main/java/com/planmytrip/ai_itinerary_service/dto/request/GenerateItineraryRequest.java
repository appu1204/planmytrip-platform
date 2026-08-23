package com.planmytrip.ai_itinerary_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * PHASE 1 - POST /api/v1/itineraries/generate
 * Payload trip-service already has when it calls us right after "Create a Trip".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateItineraryRequest {

    @NotNull(message = "tripId is required")
    private UUID tripId;

    @NotBlank(message = "destination is required")
    private String destination;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    private LocalDate endDate;

    @NotNull(message = "budget is required")
    @Positive(message = "budget must be greater than 0")
    private BigDecimal budget;

    @NotNull
    private Travellers travellers;

    // e.g. persona from user-service: Family, Solo, Couple, Friends
    @NotBlank(message = "persona is required")
    private String persona;

    // e.g. ["Backwaters", "Beaches", "Theme parks"] from the Create Trip screen
    private List<String> preferences;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Travellers {
        @Min(1)
        private int adults;

        @Min(0)
        private int children;
    }
}

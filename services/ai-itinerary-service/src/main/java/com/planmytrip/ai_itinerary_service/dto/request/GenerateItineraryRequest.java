package com.planmytrip.ai_itinerary_service.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
 * Payload sent from frontend or trip-service to generate an AI itinerary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateItineraryRequest {

    private UUID tripId;

    @NotBlank(message = "destination is required")
    private String destination;

    @JsonAlias({"checkIn", "start_date"})
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private LocalDate startDate;

    @JsonAlias({"checkOut", "end_date"})
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private LocalDate endDate;

    @NotNull(message = "budget is required")
    @Positive(message = "budget must be greater than 0")
    private BigDecimal budget;

    // Optional nested object
    private Travellers travellers;

    // Top-level fallbacks for adult/children counts
    @JsonProperty("adults")
    private Integer adults;

    @JsonProperty("children")
    private Integer children;

    // e.g. persona from user-service: Family, Solo, Couple, Friends
    private String persona;

    // e.g. ["Backwaters", "Beaches", "Theme parks"] from the Create Trip screen
    private List<String> preferences;

    public UUID getTripId() {
        if (tripId == null) {
            tripId = UUID.randomUUID();
        }
        return tripId;
    }

    public LocalDate getStartDate() {
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        return startDate;
    }

    public LocalDate getEndDate() {
        if (endDate == null) {
            endDate = getStartDate().plusDays(3);
        } else if (startDate != null && endDate.isBefore(startDate)) {
            endDate = startDate.plusDays(1);
        }
        return endDate;
    }

    public Travellers getTravellers() {
        if (travellers == null) {
            int a = (adults != null && adults >= 1) ? adults : 1;
            int c = (children != null && children >= 0) ? children : 0;
            travellers = new Travellers(a, c);
        }
        return travellers;
    }

    public String getPersona() {
        if (persona == null || persona.trim().isEmpty()) {
            return "Friends";
        }
        return persona;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Travellers {
        @Min(1)
        private int adults = 1;

        @Min(0)
        private int children = 0;
    }
}

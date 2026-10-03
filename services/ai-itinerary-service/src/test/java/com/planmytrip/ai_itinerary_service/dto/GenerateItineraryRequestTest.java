package com.planmytrip.ai_itinerary_service.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.planmytrip.ai_itinerary_service.dto.request.GenerateItineraryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class GenerateItineraryRequestTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testDeserializationWithEmptyDatesAndTopLevelTravellers() throws Exception {
        String json = """
            {
              "destination": "Goa",
              "checkIn": "",
              "checkOut": "",
              "budget": 120000,
              "adults": 2,
              "children": 0,
              "preferences": ["Backwaters", "Beaches"],
              "persona": "friends"
            }
            """;

        GenerateItineraryRequest request = objectMapper.readValue(json, GenerateItineraryRequest.class);

        assertNotNull(request);
        assertEquals("Goa", request.getDestination());
        assertEquals(new BigDecimal("120000"), request.getBudget());
        assertEquals("friends", request.getPersona());

        // tripId should auto-generate
        assertNotNull(request.getTripId());

        // dates should fall back to defaults
        assertNotNull(request.getStartDate());
        assertNotNull(request.getEndDate());
        assertTrue(request.getEndDate().isAfter(request.getStartDate()) || request.getEndDate().isEqual(request.getStartDate()));

        // travellers should be initialized from top-level adults/children
        assertNotNull(request.getTravellers());
        assertEquals(2, request.getTravellers().getAdults());
        assertEquals(0, request.getTravellers().getChildren());

        // preferences
        assertNotNull(request.getPreferences());
        assertEquals(2, request.getPreferences().size());
        assertTrue(request.getPreferences().contains("Backwaters"));
    }

    @Test
    void testDeserializationWithExplicitDatesAndNestedTravellers() throws Exception {
        String json = """
            {
              "tripId": "123e4567-e89b-12d3-a456-426614174000",
              "destination": "Paris",
              "startDate": "2026-10-01",
              "endDate": "2026-10-05",
              "budget": 200000,
              "travellers": {
                "adults": 3,
                "children": 1
              },
              "persona": "family"
            }
            """;

        GenerateItineraryRequest request = objectMapper.readValue(json, GenerateItineraryRequest.class);

        assertNotNull(request);
        assertEquals("123e4567-e89b-12d3-a456-426614174000", request.getTripId().toString());
        assertEquals(LocalDate.of(2026, 10, 1), request.getStartDate());
        assertEquals(LocalDate.of(2026, 10, 5), request.getEndDate());
        assertEquals(3, request.getTravellers().getAdults());
        assertEquals(1, request.getTravellers().getChildren());
    }
}

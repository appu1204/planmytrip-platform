package com.planmytrip.trip_service.service.impl;

import com.planmytrip.trip_service.dto.response.WeatherAdvisoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherAdvisoryServiceImplTest {

    private WeatherAdvisoryServiceImpl weatherAdvisoryService;

    @BeforeEach
    void setUp() {
        weatherAdvisoryService = new WeatherAdvisoryServiceImpl();
    }

    @Test
    void getAdvisory_ShouldReturnSafe_ForStandardDestination() {
        WeatherAdvisoryResponse response = weatherAdvisoryService.getAdvisory("Goa");

        assertNotNull(response);
        assertEquals("Goa", response.getDestination());
        assertEquals("SAFE", response.getStatus());
        assertEquals(95, response.getSafetyScore());
        assertFalse(response.isHillyTerrain());
        assertTrue(response.getHazardReasons().isEmpty());
    }

    @Test
    void getAdvisory_ShouldDetectHillyTerrain_ForHillStations() {
        WeatherAdvisoryResponse manali = weatherAdvisoryService.getAdvisory("Manali");
        assertTrue(manali.isHillyTerrain());

        WeatherAdvisoryResponse munnar = weatherAdvisoryService.getAdvisory("Munnar, Kerala");
        assertTrue(munnar.isHillyTerrain());

        WeatherAdvisoryResponse ooty = weatherAdvisoryService.getAdvisory("Ooty");
        assertTrue(ooty.isHillyTerrain());
    }

    @Test
    void getAdvisory_ShouldHandleBlankOrNullDestination() {
        WeatherAdvisoryResponse responseNull = weatherAdvisoryService.getAdvisory(null);
        assertNotNull(responseNull);
        assertEquals("your destination", responseNull.getDestination());

        WeatherAdvisoryResponse responseBlank = weatherAdvisoryService.getAdvisory("   ");
        assertNotNull(responseBlank);
        assertEquals("your destination", responseBlank.getDestination());
    }
}

package com.planmytrip.ai_itinerary_service.service;

import java.util.List;
import java.util.UUID;

import com.planmytrip.ai_itinerary_service.dto.request.GenerateItineraryRequest;
import com.planmytrip.ai_itinerary_service.dto.response.BudgetResponse;
import com.planmytrip.ai_itinerary_service.dto.response.ItineraryResponse;
import com.planmytrip.ai_itinerary_service.dto.response.ItinerarySummaryResponse;

public interface ItineraryService {

    ItineraryResponse generate(GenerateItineraryRequest request);

    ItineraryResponse save(UUID itineraryId);

    ItineraryResponse getById(UUID itineraryId);

    List<ItinerarySummaryResponse> getHistoryForTrip(UUID tripId);

    ItineraryResponse getActiveForTrip(UUID tripId);

    ItineraryResponse regenerate(UUID itineraryId, GenerateItineraryRequest overrides);

    BudgetResponse getBudgetBreakdown(UUID itineraryId);

    ItineraryResponse saveItineraryForTrip(UUID tripId, java.util.Map<String, Object> payload);

    ItineraryResponse generateForTrip(UUID tripId, GenerateItineraryRequest request);

    ItineraryResponse regenerateForTrip(UUID tripId, GenerateItineraryRequest overrides);

    BudgetResponse getBudgetForTrip(UUID tripId);

    java.util.Map<String, Object> saveNotes(UUID tripId, String notes);

    java.util.List<java.util.Map<String, Object>> getRoute(UUID tripId);

    ItineraryResponse addDay(UUID tripId, java.util.Map<String, Object> day);

    ItineraryResponse addActivity(UUID tripId, String dayId, java.util.Map<String, Object> activity);

    ItineraryResponse deleteActivity(UUID tripId, String dayId, String activityId);
}

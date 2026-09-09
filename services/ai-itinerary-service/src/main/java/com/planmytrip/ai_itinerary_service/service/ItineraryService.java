package com.planmytrip.ai_itinerary_service.service;

import java.util.List;
import java.util.UUID;

import com.planmytrip.ai_itinerary_service.dto.request.GenerateItineraryRequest;
import com.planmytrip.ai_itinerary_service.dto.response.ItineraryResponse;
import com.planmytrip.ai_itinerary_service.dto.response.ItinerarySummaryResponse;

public interface ItineraryService {

    ItineraryResponse generate(GenerateItineraryRequest request);

    ItineraryResponse save(UUID itineraryId);

    ItineraryResponse getById(UUID itineraryId);

    List<ItinerarySummaryResponse> getHistoryForTrip(UUID tripId);

    ItineraryResponse getActiveForTrip(UUID tripId);
}

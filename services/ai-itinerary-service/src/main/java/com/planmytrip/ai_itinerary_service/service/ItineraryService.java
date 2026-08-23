package com.planmytrip.ai_itinerary_service.service;

import com.planmytrip.ai_itinerary_service.dto.request.GenerateItineraryRequest;
import com.planmytrip.ai_itinerary_service.dto.response.ItineraryResponse;

public interface ItineraryService {

    ItineraryResponse generate(GenerateItineraryRequest request);
}

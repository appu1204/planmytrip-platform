package com.planmytrip.ai_itinerary_service.service;

import com.planmytrip.ai_itinerary_service.dto.request.GenerateItineraryRequest;
import com.planmytrip.ai_itinerary_service.dto.response.ItineraryPlanData;

public interface GeminiClient {

    /**
    * PHASE 1 - talks to Google Gemini and turns its response into our ItineraryPlanData model.
    * Kept separate from ItineraryService so the AI provider can be swapped later
    * (e.g. OpenAI, Claude) without touching persistence/business logic.
    */

    ItineraryPlanData generatePlan(GenerateItineraryRequest request);
}

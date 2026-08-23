package com.planmytrip.ai_itinerary_service.exception;

/** Thrown when Gemini fails to respond, times out, or returns content that can't be parsed into ItineraryPlanData. */
public class AIGenerationException extends RuntimeException {
    public AIGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AIGenerationException(String message) {
        super(message);
    }
}

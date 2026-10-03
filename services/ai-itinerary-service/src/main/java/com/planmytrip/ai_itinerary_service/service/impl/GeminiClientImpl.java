package com.planmytrip.ai_itinerary_service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planmytrip.ai_itinerary_service.dto.request.GenerateItineraryRequest;
import com.planmytrip.ai_itinerary_service.dto.response.ItineraryPlanData;
import com.planmytrip.ai_itinerary_service.exception.AIGenerationException;
import com.planmytrip.ai_itinerary_service.service.GeminiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiClientImpl implements GeminiClient {

    private final WebClient geminiWebClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.timeout-seconds}")
    private long timeoutSeconds;

    @Override
    public ItineraryPlanData generatePlan(GenerateItineraryRequest request) {

        String prompt = buildPrompt(request);

        Map<String, Object> body = Map.of(
                "contents", new Object[]{
                        Map.of(
                                "parts", new Object[]{
                                        Map.of("text", prompt)
                                }
                        )
                },
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.7
                )
        );

        JsonNode raw;

        try {

            log.info("Calling Gemini API with model: {}", model);

            String rawResponse = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{model}:generateContent")
                            .build(model)
                    )
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(t -> t instanceof WebClientResponseException &&
                                    (((WebClientResponseException) t).getStatusCode().is5xxServerError() ||
                                     ((WebClientResponseException) t).getStatusCode().value() == 429))
                            .doBeforeRetry(sig -> log.warn("Gemini API overloaded or rate limited (attempt {}). Retrying...", sig.totalRetries() + 1))
                    )
                    .timeout(
                            Duration.of(
                                    timeoutSeconds,
                                    ChronoUnit.SECONDS
                            )
                    )
                    .block();

            raw = objectMapper.readTree(rawResponse);
            log.debug("Gemini raw response: {}", raw);

        } catch (WebClientResponseException e) {

            log.error(
                    "GEMINI API ERROR - Status: {} - Response: {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );

            throw new AIGenerationException(
                    "Gemini API failed with status "
                            + e.getStatusCode().value(),
                    e
            );

        } catch (Exception e) {

            log.error(
                    "FAILED TO CALL GEMINI API: {}",
                    e.getMessage(),
                    e
            );

            throw new AIGenerationException(
                    "Failed to call Gemini API",
                    e
            );
        }

        String jsonText = extractText(raw);

        try {

            return objectMapper.readValue(
                    jsonText,
                    ItineraryPlanData.class
            );

        } catch (Exception e) {

            log.error(
                    "Gemini returned unparseable content: {}",
                    jsonText,
                    e
            );

            throw new AIGenerationException(
                    "Gemini response could not be parsed into an itinerary plan",
                    e
            );
        }
    }

    private String extractText(JsonNode raw) {

        if (raw == null) {
            throw new AIGenerationException(
                    "Gemini returned an empty response"
            );
        }

        JsonNode textNode = raw
                .path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text");

        if (textNode.isMissingNode()
                || textNode.isNull()
                || textNode.asText().isBlank()) {

            log.error("Gemini response did not contain generated text: {}", raw);

            throw new AIGenerationException(
                    "Gemini response did not contain generated content"
            );
        }

        return textNode.asText();
    }

    private String buildPrompt(GenerateItineraryRequest request) {

        long days = ChronoUnit.DAYS.between(
                request.getStartDate(),
                request.getEndDate()
        ) + 1;

        return """
                You are a travel planning assistant for the app "PlanMyTrip".

                Generate a complete day-by-day travel itinerary as STRICT JSON.

                Return ONLY valid JSON.
                Do not use markdown.
                Do not use code fences.
                Do not add explanations before or after the JSON.

                The JSON must match exactly this schema:

                {
                  "days": [
                    {
                      "dayNumber": 1,
                      "title": "string",
                      "date": "yyyy-MM-dd",
                      "activities": [
                        {
                          "time": "10:00 AM",
                          "title": "string",
                          "description": "string",
                          "tag": "string"
                        }
                      ]
                    }
                  ],
                  "budgetBreakdown": {
                    "stays": 0,
                    "activities": 0,
                    "transport": 0,
                    "buffer": 0,
                    "total": 0,
                    "currency": "INR"
                  },
                  "preferencesUsed": ["string"],
                  "aiSummary": "one short sentence describing the trip"
                }

                Trip details:

                - Destination: %s
                - Start date: %s
                - End date: %s
                - Duration: %d days
                - Traveller persona: %s
                - Travellers: %d adults, %d children
                - Total budget: %s INR
                - Preferences: %s

                Rules:

                - Number of "days" entries MUST equal %d.
                - Dates must be consecutive and start from the start date.
                - Each day should have suitable activities for the destination.
                - Tailor the itinerary to the traveller persona.
                - Family: kid-friendly and slower pace.
                - Adventure: active and outdoor activities.
                - Solo: flexible and independent.
                - Couple: romantic experiences.
                - Friends: group and social activities.
                - Respect the preferences provided.
                - Budget breakdown values should be realistic.
                - budgetBreakdown total should approximately equal the total trip budget.
                - Return ONLY valid JSON.
                """.formatted(
                request.getDestination(),
                request.getStartDate(),
                request.getEndDate(),
                days,
                request.getPersona(),
                request.getTravellers().getAdults(),
                request.getTravellers().getChildren(),
                request.getBudget(),
                request.getPreferences() == null
                        || request.getPreferences().isEmpty()
                        ? "none specified"
                        : String.join(", ", request.getPreferences()),
                days
        );
    }
}
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

        if (apiKey == null || apiKey.trim().isEmpty() || "dummy".equalsIgnoreCase(apiKey.trim())) {
            log.warn("Gemini API key is not configured. Generating curated fallback itinerary for {}", request.getDestination());
            return generateFallbackPlan(request);
        }

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
                    .retryWhen(reactor.util.retry.Retry.backoff(2, Duration.ofSeconds(1))
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

            String jsonText = extractText(raw);
            return objectMapper.readValue(jsonText, ItineraryPlanData.class);

        } catch (Exception e) {
            log.warn("Gemini AI API call or parsing failed ({}). Providing curated fallback itinerary for: {}",
                    e.getMessage(), request.getDestination());
            return generateFallbackPlan(request);
        }
    }

    private ItineraryPlanData generateFallbackPlan(GenerateItineraryRequest request) {
        String destination = (request.getDestination() != null && !request.getDestination().isBlank())
                ? request.getDestination().trim()
                : "Your Destination";
        String persona = request.getPersona() != null ? request.getPersona() : "Solo";
        java.util.List<String> prefs = request.getPreferences() != null && !request.getPreferences().isEmpty()
                ? request.getPreferences()
                : java.util.List.of("Sightseeing", "Local Cuisine");

        java.time.LocalDate start = request.getStartDate();
        java.time.LocalDate end = request.getEndDate();
        int totalDays = (int) (java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1);
        if (totalDays <= 0) totalDays = 3;
        if (totalDays > 14) totalDays = 14;

        String normDest = destination.toLowerCase().replaceAll("[^a-z0-9]", "");

        // Destination-specific curated landmark schedules for top destinations
        java.util.List<String[]> destDayTemplates = new java.util.ArrayList<>();

        if (normDest.contains("goa")) {
            destDayTemplates.add(new String[]{
                "Candolim Beach & Fort Aguada Sunset",
                "09:30 AM", "Fort Aguada & Lighthouse Viewpoint", "Historic 17th-century Portuguese coastal fortress overlooking the Arabian Sea.", "Sightseeing",
                "01:00 PM", "Candolim Beach Promenade & Seafood Lunch", "Savor authentic Goan fish curry, prawn balchao, and coconut coolers.", "Dining",
                "04:30 PM", "Sinquerim Beach Walk & Water Sports", "Jet skiing and banana boat rides along the scenic coastline.", "Adventure",
                "07:30 PM", "Sunset Dinner at Fisherman's Wharf", "Coastal candlelight dining with live acoustic music.", "Leisure"
            });
            destDayTemplates.add(new String[]{
                "North Goa Waves & Clifftop Panoramas",
                "09:00 AM", "Chapora Fort & Dil Chahta Hai Viewpoint", "Elevated ramparts with sweeping vistas over Vagator and Morjim beach.", "Sightseeing",
                "11:30 AM", "Anjuna Flea Market & Beach Exploration", "Browse handcrafted jewelry, beachwear, and indie café treasures.", "Shopping",
                "02:00 PM", "Thalassa Clifftop Mediterranean Lunch", "Scenic dining perched on the dramatic red cliffs overlooking the sea.", "Dining",
                "05:30 PM", "Baga Beach Promenade & Coastal Sundowner", "Vibrant waves, beachfront shacks, and live sunset beats.", "Leisure"
            });
            destDayTemplates.add(new String[]{
                "Old Goa Baroque Cathedrals & Latin Quarter",
                "09:30 AM", "Basilica of Bom Jesus & Se Cathedral", "UNESCO World Heritage churches holding sacred relics of St. Francis Xavier.", "Heritage",
                "12:30 PM", "Fontainhas Latin Quarter Heritage Walk", "Wander amidst colorful Portuguese villas, azulejo tiles, and heritage balconies.", "Culture",
                "02:00 PM", "Traditional Lunch at Viva Panjim", "Authentic chicken cafreal, pork vindaloo, and warm bebinca dessert.", "Dining",
                "05:30 PM", "Mandovi River Sunset Cruise", "Evening river cruise featuring traditional Goan Dekhni folk performances.", "Leisure"
            });
            destDayTemplates.add(new String[]{
                "Spice Plantation & South Goa Coastal Charms",
                "09:30 AM", "Sahakari Spice Plantation Guided Tour", "Discover cardamom, vanilla, and cinnamon groves followed by a banana-leaf feast.", "Culture",
                "02:30 PM", "Colva Beach & Coastal Palms Walk", "Relax on powder-soft sands beneath swaying coconut palms.", "Leisure",
                "05:30 PM", "Palolem Beach Crescent Bay Sunset", "Picturesque tranquil bay with dolphin spotting and sunset boat rides.", "Sightseeing",
                "08:00 PM", "Beachside Bonfire Dinner under the Stars", "Fresh grilled lobster and soothing sound of ocean waves.", "Dining"
            });
        } else if (normDest.contains("kashmir") || normDest.contains("srinagar")) {
            destDayTemplates.add(new String[]{
                "Dal Lake Arrival & Mughal Gardens",
                "10:00 AM", "Dal Lake Houseboat Check-in & Kahwa", "Traditional cedar houseboat with aromatic saffron tea and almond cookies.", "Leisure",
                "02:00 PM", "Nishat Bagh & Shalimar Bagh Mughal Terraces", "Cascading fountains, royal chinar trees, and vibrant flowerbeds.", "Sightseeing",
                "05:30 PM", "Sunset Shikara Ride to Char Chinar", "Gliding across glassy waters with Zabarwan mountain reflections.", "Leisure",
                "08:00 PM", "Authentic Kashmiri Wazwan Feast", "Multi-course royal feast: Rogan Josh, Gushtaba, and Rista.", "Dining"
            });
            destDayTemplates.add(new String[]{
                "Gulmarg Alpine Meadows & Gondola Heights",
                "08:30 AM", "Scenic Drive to Gulmarg 'Meadow of Flowers'", "Winding alpine roads ascending into pine forests and snow peaks.", "Adventure",
                "11:00 AM", "Gulmarg Gondola to Apharwat Peak", "Ascend to 13,780 feet on one of the world's highest cable cars.", "Adventure",
                "02:00 PM", "Alpine Lunch & St. Mary's Stone Church", "Hot mutton yakhni and dum aloo overlooking rolling green meadows.", "Dining",
                "05:00 PM", "Golf Course Meadow Walk & Return", "Peaceful stroll through crisp mountain air and wildflower fields.", "Sightseeing"
            });
            destDayTemplates.add(new String[]{
                "Pahalgam Valley & Betaab Valley",
                "08:00 AM", "Pampore Saffron Fields & Drive to Pahalgam", "Purple saffron blooms and cricket bat willow workshops en route.", "Sightseeing",
                "11:30 AM", "Betaab Valley & Aru Valley Exploration", "Crystal clear Lidder River winding through lush pine-clad hills.", "Nature",
                "02:30 PM", "Lidder Riverside Fresh Trout Lunch", "Freshly prepared trout with mountain herbs alongside the river.", "Dining",
                "05:00 PM", "Pony Trek to Baisaran 'Mini Switzerland'", "Expansive green plateau surrounded by dense Himalayan deodar forests.", "Adventure"
            });
        } else if (normDest.contains("kerala") || normDest.contains("kochi") || normDest.contains("alleppey") || normDest.contains("munnar")) {
            destDayTemplates.add(new String[]{
                "Fort Kochi Colonial Heritage & Chinese Nets",
                "09:30 AM", "Chinese Fishing Nets & Mattancherry Jew Town", "Historic cantilevered fishing nets and 400-year-old spice warehouses.", "Heritage",
                "01:00 PM", "Malabar Seafood Lunch at Fort Kochi", "Karimeen Pollichathu (pearl spot fish in banana leaf) with fluffy appams.", "Dining",
                "03:30 PM", "Santa Cruz Cathedral Basilica & Dutch Palace", "Restored colonial architecture, royal murals, and heritage courtyards.", "Sightseeing",
                "06:00 PM", "Kathakali Classical Dance & Kalaripayattu Show", "Vibrant eye expressions, intricate makeup, and ancient martial arts.", "Culture"
            });
            destDayTemplates.add(new String[]{
                "Munnar Misty Tea Hills & Eravikulam",
                "08:30 AM", "Scenic Western Ghats Drive to Munnar", "Cascading Cheeyappara Waterfalls and winding emerald mountain slopes.", "Nature",
                "11:30 AM", "Tea Museum & Factory Processing Tour", "Secrets of orthodox tea crafting followed by fresh tasting.", "Culture",
                "02:30 PM", "Eravikulam National Park Nilgiri Tahr Safari", "Spot endangered mountain goats across high-altitude rolling grasslands.", "Adventure",
                "05:30 PM", "Mattupetty Dam & Echo Point Boating", "Tranquil lake nestled between mist-kissed Shola hills.", "Sightseeing"
            });
            destDayTemplates.add(new String[]{
                "Alleppey Vembanad Lake Houseboat Cruise",
                "11:30 AM", "Board Traditional Kettuvallam Houseboat", "Thatch-roofed luxury wooden boat with private chef and captain.", "Leisure",
                "01:30 PM", "Backwater Canal Lunch Cruise", "Freshly cooked Kerala fish curry and tiger prawns as palms drift past.", "Dining",
                "04:00 PM", "Village Canoe Ride in Hidden Waterways", "Witness coir yarn spinning, duck farming, and backwater hamlet life.", "Culture",
                "07:30 PM", "Houseboat Starlit Mooring on Vembanad Lake", "Serene evening with gentle lapping of calm waters.", "Leisure"
            });
        }

        java.util.List<ItineraryPlanData.DayPlan> days = new java.util.ArrayList<>();

        for (int i = 0; i < totalDays; i++) {
            java.time.LocalDate dayDate = start.plusDays(i);
            ItineraryPlanData.DayPlan day = new ItineraryPlanData.DayPlan();
            day.setDayNumber(i + 1);
            day.setDate(dayDate.toString());

            java.util.List<ItineraryPlanData.Activity> acts = new java.util.ArrayList<>();

            if (!destDayTemplates.isEmpty()) {
                String[] template = destDayTemplates.get(i % destDayTemplates.size());
                day.setTitle(template[0] + (totalDays > destDayTemplates.size() ? " - Part " + ((i / destDayTemplates.size()) + 1) : ""));
                acts.add(new ItineraryPlanData.Activity(template[1], template[2], template[3], template[4]));
                acts.add(new ItineraryPlanData.Activity(template[5], template[6], template[7], template[8]));
                acts.add(new ItineraryPlanData.Activity(template[9], template[10], template[11], template[12]));
                acts.add(new ItineraryPlanData.Activity(template[13], template[14], template[15], template[16]));
            } else {
                String[] genericTitles = {
                    "Arrival & Heritage Discovery in " + destination,
                    "Cultural Landmarks, Local Markets & Gastronomy",
                    "Scenic Corridors, Nature Views & Relaxation",
                    "Artisan Trails & Panoramic Viewpoints",
                    "Hidden Alleys, Coastal/Mountain Breeze & Leisure",
                    "Local Flavors & Evening Entertainment",
                    "Memorable Highlights & Farewell Panorama"
                };
                day.setTitle(genericTitles[i % genericTitles.length]);
                acts.add(new ItineraryPlanData.Activity("09:00 AM", "Morning Scenic Trail & Breakfast in " + destination,
                        "Kick off the day with traditional morning refreshments and an atmospheric landmark stroll.", "Morning Trail"));
                acts.add(new ItineraryPlanData.Activity("12:30 PM", "Signature Regional Cuisine Tasting",
                        "Enjoy an authentic lunch savoring celebrated local flavors tailored for " + persona + " travellers.", "Dining"));
                acts.add(new ItineraryPlanData.Activity("03:30 PM", "Historic Landmark & Architectural Exploration",
                        "Explore famous viewpoints and heritage monuments with captivating photography spots.", "Sightseeing"));
                acts.add(new ItineraryPlanData.Activity("07:00 PM", "Sunset Golden Hour & Evening Ambience",
                        "Unwind at a popular promenade or venue taking in panoramic twilight vistas.", "Leisure"));
            }

            day.setActivities(acts);
            days.add(day);
        }

        java.math.BigDecimal budget = request.getBudget();
        java.math.BigDecimal stays = budget.multiply(new java.math.BigDecimal("0.40")).setScale(2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal activities = budget.multiply(new java.math.BigDecimal("0.30")).setScale(2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal transport = budget.multiply(new java.math.BigDecimal("0.20")).setScale(2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal buffer = budget.subtract(stays).subtract(activities).subtract(transport);

        ItineraryPlanData.BudgetBreakdown breakdown = new ItineraryPlanData.BudgetBreakdown(
                stays, activities, transport, buffer, budget, "INR"
        );

        ItineraryPlanData plan = new ItineraryPlanData();
        plan.setDays(days);
        plan.setBudgetBreakdown(breakdown);
        plan.setPreferencesUsed(prefs);
        plan.setAiSummary("Tailored " + totalDays + "-day " + persona.toLowerCase() + " itinerary for " + destination + ".");
        plan.setNotes("Curated with recommended timings, dining highlights, and authentic landmark stops.");
        return plan;
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
                You are a world-class travel planning expert for the app "PlanMyTrip".

                Generate an authentic, high-quality, day-by-day travel itinerary as STRICT JSON.

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

                CRITICAL RULES:
                - ALWAYS use SPECIFIC, REAL, ACCURATE LANDMARK and ATTRACTION NAMES in activity titles (e.g. "Visit Fort Aguada & Lighthouse", "Relax at Baga Beach Promenade", "Tour Basilica of Bom Jesus in Old Goa", "Explore Fontainhas Latin Quarter", rather than generic titles like "Morning Trail" or "Sightseeing Stop").
                - Number of "days" entries MUST equal %d.
                - Dates must be consecutive and start from the start date.
                - Group activities geographically per day to minimize travel time.
                - Tailor pacing to the %s persona and respect preferences (%s).
                - Budget breakdown values must sum up to approximately %s INR.
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
                days,
                request.getPersona(),
                request.getPreferences() == null ? "Standard" : String.join(", ", request.getPreferences()),
                request.getBudget()
        );
    }
}
package com.planmytrip.ai_itinerary_service.service.impl;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.planmytrip.ai_itinerary_service.dto.request.GenerateItineraryRequest;
import com.planmytrip.ai_itinerary_service.dto.response.BudgetResponse;
import com.planmytrip.ai_itinerary_service.dto.response.ItineraryPlanData;
import com.planmytrip.ai_itinerary_service.dto.response.ItineraryResponse;
import com.planmytrip.ai_itinerary_service.dto.response.ItinerarySummaryResponse;
import com.planmytrip.ai_itinerary_service.entity.Itinerary;
import com.planmytrip.ai_itinerary_service.enums.ItineraryStatus;
import com.planmytrip.ai_itinerary_service.exception.ResourceNotFoundException;
import com.planmytrip.ai_itinerary_service.repository.ItineraryRepository;
import com.planmytrip.ai_itinerary_service.security.CurrentUserProvider;
import com.planmytrip.ai_itinerary_service.service.GeminiClient;
import com.planmytrip.ai_itinerary_service.service.ItineraryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor

public class ItineraryServiceImpl implements ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final GeminiClient geminiClient;
    private final CurrentUserProvider currentUserProvider;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;


    /** PHASE 1 — Generate a day-by-day travel plan
     * Endpoint: POST /api/v1/ai-itinerary/generate
     * Creates a DRAFT (version 1). Nothing is marked "active" until /save is called.
    */

    @Transactional
    public ItineraryResponse generate(GenerateItineraryRequest request) {
        Long userId = currentUserProvider.getUserId();

        // Normalize request values
        request.setTripId(request.getTripId());
        request.setStartDate(request.getStartDate());
        request.setEndDate(request.getEndDate());
        request.setTravellers(request.getTravellers());
        request.setPersona(request.getPersona());

        ItineraryPlanData planData = geminiClient.generatePlan(request);

        int durationDays = (int) (ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1);

        Itinerary itinerary = Itinerary.builder()
                .tripId(request.getTripId())
                .userId(userId)
                .destination(request.getDestination())
                .durationDays(durationDays)
                .totalBudget(request.getBudget())
                .status(ItineraryStatus.DRAFT)
                .version(1)
                .active(false)
                .planData(planData)
                .build();

        Itinerary saved = itineraryRepository.save(itinerary);
        log.info("Generated itinerary {} (v1) for trip {} / user {}", saved.getId(), saved.getTripId(), userId);

        return ItineraryResponse.fromEntity(saved);
    }

    /**
     * phase 2 - confirm/save a generated draft as the trip's active itinerary
     * Endpoint: PATCH /api/v1/ai-itinerary{itineraryId}/save
     * save the generated plan
     */


    @Transactional
    public ItineraryResponse save(UUID itineraryId) {
        Long userId = currentUserProvider.getUserId();
        Itinerary itinerary = getOwnedOrThrow(itineraryId, userId);

        // only one active/saved itinerary per trip — deactivate any previous one
        itineraryRepository.findByTripIdAndActiveTrue(itinerary.getTripId())
                .filter(existing -> !existing.getId().equals(itinerary.getId()))
                .ifPresent(existing -> {
                    existing.setActive(false);
                    itineraryRepository.save(existing);
                });

        itinerary.setStatus(ItineraryStatus.SAVED);
        itinerary.setActive(true);

        Itinerary saved = itineraryRepository.save(itinerary);
        log.info("Saved itinerary {} as active plan for trip {}", saved.getId(), saved.getTripId());

        return ItineraryResponse.fromEntity(saved);
    }

    /**
     * view the full plan of trip
     * GET /api/v1/ai-itinerary/{itineraryId}          -> one full itinerary
     * GET /api/v1/ai-itinerary/trip/{tripId}          -> version history (summaries)
     * GET /api/v1/ai-itinerary/trip/{tripId}/active   -> the currently saved/active plan
     */


    @Transactional (readOnly = true)
    public ItineraryResponse getById(UUID itineraryId) {
        Long userId = currentUserProvider.getUserId();
        return ItineraryResponse.fromEntity(getOwnedOrThrow(itineraryId, userId));
    }

    @Transactional(readOnly = true)
    public List<ItinerarySummaryResponse> getHistoryForTrip(UUID tripId) {
        return itineraryRepository.findByTripIdOrderByVersionDesc(tripId).stream()
                .map(ItinerarySummaryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ItineraryResponse getActiveForTrip(UUID tripId) {
        Long userId = currentUserProvider.getUserId();

        Itinerary itinerary = itineraryRepository.findByTripIdAndActiveTrue(tripId)
                .or(() -> itineraryRepository.findTopByTripIdOrderByVersionDesc(tripId))
                .orElse(null);

        if (itinerary == null) {
            throw new ResourceNotFoundException("No itinerary found for trip " + tripId);
        }

        return ItineraryResponse.fromEntity(itinerary);
    }


    // =========================================================================
    // PHASE 4 — Regenerate the plan
    // Endpoint: POST /api/v1/itineraries/{itineraryId}/regenerate
    // Calls Gemini again and stores it as a NEW version/row — the old draft/plan
    // is left untouched so the user never loses a version they liked.
    // =========================================================================
    
    @Transactional
    public ItineraryResponse regenerate(UUID itineraryId, GenerateItineraryRequest overrides) {
        Long userId = currentUserProvider.getUserId();
        Itinerary previous = getOwnedOrThrow(itineraryId, userId);

        // allow the caller to tweak preferences/budget on regenerate; fall back to the previous request's values
        GenerateItineraryRequest request = overrides != null ? overrides : rebuildRequestFrom(previous);

        ItineraryPlanData newPlan = geminiClient.generatePlan(request);

        int nextVersion = itineraryRepository.findTopByTripIdOrderByVersionDesc(previous.getTripId())
                .map(i -> i.getVersion() + 1)
                .orElse(previous.getVersion() + 1);

        Itinerary regenerated = Itinerary.builder()
                .tripId(previous.getTripId())
                .userId(userId)
                .destination(request.getDestination())
                .durationDays(previous.getDurationDays())
                .totalBudget(request.getBudget())
                .status(ItineraryStatus.DRAFT)
                .version(nextVersion)
                .active(false)
                .planData(newPlan)
                .build();

        Itinerary saved = itineraryRepository.save(regenerated);
        log.info("Regenerated itinerary for trip {} -> new version {} (itinerary {})",
                previous.getTripId(), nextVersion, saved.getId());

        return ItineraryResponse.fromEntity(saved);
    }

    /**
     *PHASE 5 — Estimated budget breakdown
     * Endpoint: GET /api/v1/itineraries/{itineraryId}/budget
     * The breakdown is already produced by Gemini during generation (see
     * ItineraryPlanData.budgetBreakdown); this just exposes it as its own
     * lightweight resource for the "Estimated cost" widget on the frontend.
    */
   
    @Transactional(readOnly = true)
    public BudgetResponse getBudgetBreakdown(UUID itineraryId) {
        Long userId = currentUserProvider.getUserId();
        Itinerary itinerary = getOwnedOrThrow(itineraryId, userId);
        return BudgetResponse.fromEntity(itinerary);
    }

    /**
     * Helper method
     * throws NotFoundException if the itinerary is not found or not owned by the user
     */
    
    private Itinerary getOwnedOrThrow(UUID itineraryId, Long userId) {
        return itineraryRepository.findByIdAndUserId(itineraryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Itinerary " + itineraryId + " not found for the current user"));
    }

    private GenerateItineraryRequest rebuildRequestFrom(Itinerary previous) {
        GenerateItineraryRequest request = new GenerateItineraryRequest();
        request.setTripId(previous.getTripId());
        request.setDestination(previous.getDestination());
        request.setBudget(previous.getTotalBudget());
        request.setStartDate(java.time.LocalDate.now());
        request.setEndDate(java.time.LocalDate.now().plusDays(previous.getDurationDays() - 1));
        request.setPersona("Family"); // sensible default; prefer passing `overrides` explicitly from the client
        GenerateItineraryRequest.Travellers travellers = new GenerateItineraryRequest.Travellers();
        travellers.setAdults(2);
        travellers.setChildren(0);
        request.setTravellers(travellers);
        request.setPreferences(previous.getPlanData() != null ? previous.getPlanData().getPreferencesUsed() : null);
        return request;
    }

    @Override
    @Transactional
    public ItineraryResponse saveItineraryForTrip(UUID tripId, java.util.Map<String, Object> payload) {
        Long userId = currentUserProvider.getUserId();

        UUID itineraryId = null;
        if (payload != null) {
            if (payload.get("itineraryId") != null) {
                try { itineraryId = UUID.fromString(payload.get("itineraryId").toString()); } catch (Exception ignored) {}
            }
            if (itineraryId == null && payload.get("id") != null) {
                try { itineraryId = UUID.fromString(payload.get("id").toString()); } catch (Exception ignored) {}
            }
        }

        Itinerary itinerary = null;
        if (itineraryId != null) {
            itinerary = itineraryRepository.findById(itineraryId).orElse(null);
        }

        if (itinerary == null) {
            itinerary = itineraryRepository.findByTripIdAndActiveTrue(tripId)
                    .or(() -> itineraryRepository.findTopByTripIdOrderByVersionDesc(tripId))
                    .orElse(null);
        }

        if (itinerary != null) {
            itinerary.setTripId(tripId);
            itinerary.setActive(true);
            itinerary.setStatus(ItineraryStatus.SAVED);

            if (payload != null && payload.get("days") != null) {
                try {
                    List<ItineraryPlanData.DayPlan> days = objectMapper.convertValue(
                            payload.get("days"),
                            new com.fasterxml.jackson.core.type.TypeReference<List<ItineraryPlanData.DayPlan>>() {}
                    );
                    if (days != null && !days.isEmpty()) {
                        if (itinerary.getPlanData() == null) {
                            itinerary.setPlanData(new ItineraryPlanData());
                        }
                        itinerary.getPlanData().setDays(days);
                    }
                } catch (Exception e) {
                    log.warn("Could not deserialize days in saveItineraryForTrip: {}", e.getMessage());
                }
            }

            Itinerary saved = itineraryRepository.save(itinerary);
            log.info("Saved itinerary {} as active for trip {}", saved.getId(), tripId);
            return ItineraryResponse.fromEntity(saved);
        }

        // If no itinerary existed yet, create one from the payload
        if (payload != null && payload.get("days") != null) {
            try {
                List<ItineraryPlanData.DayPlan> days = objectMapper.convertValue(
                        payload.get("days"),
                        new com.fasterxml.jackson.core.type.TypeReference<List<ItineraryPlanData.DayPlan>>() {}
                );
                String destination = payload.get("destination") != null ? payload.get("destination").toString() : "Trip";
                java.math.BigDecimal budget = payload.get("totalBudget") != null
                        ? new java.math.BigDecimal(payload.get("totalBudget").toString())
                        : java.math.BigDecimal.valueOf(30000);

                ItineraryPlanData pd = new ItineraryPlanData();
                pd.setDays(days);

                Itinerary newItinerary = Itinerary.builder()
                        .tripId(tripId)
                        .userId(userId)
                        .destination(destination)
                        .durationDays(days != null ? days.size() : 3)
                        .totalBudget(budget)
                        .status(ItineraryStatus.SAVED)
                        .version(1)
                        .active(true)
                        .planData(pd)
                        .build();

                Itinerary saved = itineraryRepository.save(newItinerary);
                log.info("Created new saved itinerary {} for trip {}", saved.getId(), tripId);
                return ItineraryResponse.fromEntity(saved);
            } catch (Exception e) {
                log.error("Failed to construct itinerary from payload: {}", e.getMessage());
            }
        }

        throw new ResourceNotFoundException("No itinerary found to save for trip " + tripId);
    }

    @Override
    @Transactional
    public ItineraryResponse generateForTrip(UUID tripId, GenerateItineraryRequest request) {
        if (request == null) {
            request = new GenerateItineraryRequest();
        }
        request.setTripId(tripId);
        if (request.getDestination() == null || request.getDestination().isBlank()) {
            request.setDestination("Trip");
        }
        if (request.getBudget() == null) {
            request.setBudget(java.math.BigDecimal.valueOf(30000));
        }

        ItineraryResponse response = generate(request);
        save(response.getItineraryId());
        return getById(response.getItineraryId());
    }

    @Override
    @Transactional
    public ItineraryResponse regenerateForTrip(UUID tripId, GenerateItineraryRequest overrides) {
        Itinerary active = itineraryRepository.findByTripIdAndActiveTrue(tripId)
                .or(() -> itineraryRepository.findTopByTripIdOrderByVersionDesc(tripId))
                .orElse(null);

        if (active != null) {
            ItineraryResponse res = regenerate(active.getId(), overrides);
            save(res.getItineraryId());
            return getById(res.getItineraryId());
        }

        return generateForTrip(tripId, overrides);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getBudgetForTrip(UUID tripId) {
        Itinerary itinerary = itineraryRepository.findByTripIdAndActiveTrue(tripId)
                .or(() -> itineraryRepository.findTopByTripIdOrderByVersionDesc(tripId))
                .orElseThrow(() -> new ResourceNotFoundException("No itinerary found for trip " + tripId));
        return BudgetResponse.fromEntity(itinerary);
    }

    @Override
    @Transactional
    public java.util.Map<String, Object> saveNotes(UUID tripId, String notes) {
        return java.util.Map.of("tripId", tripId.toString(), "notes", notes != null ? notes : "", "saved", true);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getRoute(UUID tripId) {
        ItineraryResponse active = getActiveForTrip(tripId);
        java.util.List<java.util.Map<String, Object>> stops = new java.util.ArrayList<>();
        if (active != null && active.getDays() != null) {
            for (ItineraryPlanData.DayPlan day : active.getDays()) {
                java.util.Map<String, Object> stop = new java.util.HashMap<>();
                stop.put("id", day.getId());
                stop.put("name", day.getLabel());
                stop.put("dayLabel", "Day " + day.getIndex());
                String note = (day.getActivities() != null && !day.getActivities().isEmpty())
                        ? day.getActivities().get(0).getTitle() : "";
                stop.put("note", note);
                stops.add(stop);
            }
        }
        return stops;
    }

    @Override
    @Transactional
    public ItineraryResponse addDay(UUID tripId, java.util.Map<String, Object> dayPayload) {
        Itinerary itinerary = itineraryRepository.findByTripIdAndActiveTrue(tripId)
                .or(() -> itineraryRepository.findTopByTripIdOrderByVersionDesc(tripId))
                .orElseThrow(() -> new ResourceNotFoundException("No itinerary found for trip " + tripId));

        if (itinerary.getPlanData() == null) {
            itinerary.setPlanData(new ItineraryPlanData());
        }
        if (itinerary.getPlanData().getDays() == null) {
            itinerary.getPlanData().setDays(new java.util.ArrayList<>());
        }

        try {
            ItineraryPlanData.DayPlan day = objectMapper.convertValue(dayPayload, ItineraryPlanData.DayPlan.class);
            if (day != null) {
                if (day.getDayNumber() <= 0) {
                    day.setDayNumber(itinerary.getPlanData().getDays().size() + 1);
                }
                itinerary.getPlanData().getDays().add(day);
                itinerary.setDurationDays(itinerary.getPlanData().getDays().size());
            }
        } catch (Exception e) {
            log.warn("Failed to parse added day: {}", e.getMessage());
        }

        Itinerary saved = itineraryRepository.save(itinerary);
        return ItineraryResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public ItineraryResponse addActivity(UUID tripId, String dayId, java.util.Map<String, Object> activityPayload) {
        Itinerary itinerary = itineraryRepository.findByTripIdAndActiveTrue(tripId)
                .or(() -> itineraryRepository.findTopByTripIdOrderByVersionDesc(tripId))
                .orElseThrow(() -> new ResourceNotFoundException("No itinerary found for trip " + tripId));

        if (itinerary.getPlanData() != null && itinerary.getPlanData().getDays() != null) {
            for (ItineraryPlanData.DayPlan d : itinerary.getPlanData().getDays()) {
                if (d.getId().equalsIgnoreCase(dayId) || String.valueOf(d.getDayNumber()).equals(dayId)) {
                    if (d.getActivities() == null) {
                        d.setActivities(new java.util.ArrayList<>());
                    }
                    try {
                        ItineraryPlanData.Activity act = objectMapper.convertValue(activityPayload, ItineraryPlanData.Activity.class);
                        if (act != null) {
                            d.getActivities().add(act);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse added activity: {}", e.getMessage());
                    }
                    break;
                }
            }
        }

        Itinerary saved = itineraryRepository.save(itinerary);
        return ItineraryResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public ItineraryResponse deleteActivity(UUID tripId, String dayId, String activityId) {
        Itinerary itinerary = itineraryRepository.findByTripIdAndActiveTrue(tripId)
                .or(() -> itineraryRepository.findTopByTripIdOrderByVersionDesc(tripId))
                .orElseThrow(() -> new ResourceNotFoundException("No itinerary found for trip " + tripId));

        if (itinerary.getPlanData() != null && itinerary.getPlanData().getDays() != null) {
            for (ItineraryPlanData.DayPlan d : itinerary.getPlanData().getDays()) {
                if (d.getId().equalsIgnoreCase(dayId) || String.valueOf(d.getDayNumber()).equals(dayId)) {
                    if (d.getActivities() != null) {
                        d.getActivities().removeIf(a -> a.getId().equalsIgnoreCase(activityId));
                    }
                    break;
                }
            }
        }

        Itinerary saved = itineraryRepository.save(itinerary);
        return ItineraryResponse.fromEntity(saved);
    }
}

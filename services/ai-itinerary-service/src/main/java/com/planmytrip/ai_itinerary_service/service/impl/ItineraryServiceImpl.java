package com.planmytrip.ai_itinerary_service.service.impl;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.planmytrip.ai_itinerary_service.dto.request.GenerateItineraryRequest;
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


    /** PHASE 1 — Generate a day-by-day travel plan
     * Endpoint: POST /api/v1/ai-itinerary/generate
     * Creates a DRAFT (version 1). Nothing is marked "active" until /save is called.
    */

    @Transactional
    public ItineraryResponse generate(GenerateItineraryRequest request) {
        Long userId = currentUserProvider.getUserId();

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
        Itinerary itinerary = itineraryRepository.findByTripIdAndActiveTrue(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("No saved itinerary found for trip " + tripId));
        return ItineraryResponse.fromEntity(itinerary);
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
}

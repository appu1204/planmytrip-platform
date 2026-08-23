package com.planmytrip.ai_itinerary_service.service.impl;

import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.planmytrip.ai_itinerary_service.dto.request.GenerateItineraryRequest;
import com.planmytrip.ai_itinerary_service.dto.response.ItineraryPlanData;
import com.planmytrip.ai_itinerary_service.dto.response.ItineraryResponse;
import com.planmytrip.ai_itinerary_service.entity.Itinerary;
import com.planmytrip.ai_itinerary_service.enums.ItineraryStatus;
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
     * Endpoint: POST /api/v1/itineraries/generate
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
}

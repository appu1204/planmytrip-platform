package com.planmytrip.ai_itinerary_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planmytrip.ai_itinerary_service.service.GeminiClient;
import com.planmytrip.ai_itinerary_service.dto.response.BudgetResponse;
import com.planmytrip.ai_itinerary_service.dto.response.ItineraryPlanData;
import com.planmytrip.ai_itinerary_service.dto.response.ItineraryResponse;
import com.planmytrip.ai_itinerary_service.dto.response.ItinerarySummaryResponse;
import com.planmytrip.ai_itinerary_service.entity.Itinerary;
import com.planmytrip.ai_itinerary_service.enums.ItineraryStatus;
import com.planmytrip.ai_itinerary_service.exception.ResourceNotFoundException;
import com.planmytrip.ai_itinerary_service.exception.UnauthorizedException;
import com.planmytrip.ai_itinerary_service.repository.ItineraryRepository;
import com.planmytrip.ai_itinerary_service.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItineraryServiceImplTest {

    @Mock
    private ItineraryRepository itineraryRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ItineraryServiceImpl itineraryService;

    private UUID tripId;
    private UUID itineraryId;
    private Long ownerId = 100L;
    private Long otherUserId = 200L;
    private Itinerary testItinerary;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        itineraryId = UUID.randomUUID();

        ItineraryPlanData.BudgetBreakdown budget = new ItineraryPlanData.BudgetBreakdown();
        budget.setTotal(BigDecimal.valueOf(30000));
        budget.setCurrency("INR");

        ItineraryPlanData planData = new ItineraryPlanData();
        planData.setBudgetBreakdown(budget);
        planData.setDays(List.of());

        testItinerary = Itinerary.builder()
                .id(itineraryId)
                .tripId(tripId)
                .userId(ownerId)
                .destination("Goa")
                .durationDays(5)
                .totalBudget(BigDecimal.valueOf(30000))
                .status(ItineraryStatus.SAVED)
                .version(1)
                .active(true)
                .planData(planData)
                .build();
    }

    @Test
    void getActiveForTrip_ShouldSucceed_WhenOwnerRequests() {
        when(currentUserProvider.getUserId()).thenReturn(ownerId);
        when(itineraryRepository.findByTripIdAndActiveTrue(tripId)).thenReturn(Optional.of(testItinerary));

        ItineraryResponse response = itineraryService.getActiveForTrip(tripId);

        assertNotNull(response);
        assertEquals(itineraryId, response.getItineraryId());
        assertEquals("Goa", response.getDestination());
    }

    @Test
    void getActiveForTrip_ShouldThrowNotFound_WhenNoItineraryFound() {
        when(currentUserProvider.getUserId()).thenReturn(ownerId);
        when(itineraryRepository.findByTripIdAndActiveTrue(tripId)).thenReturn(Optional.empty());
        when(itineraryRepository.findTopByTripIdOrderByVersionDesc(tripId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> itineraryService.getActiveForTrip(tripId));
    }

    @Test
    void getActiveForTrip_ShouldThrowUnauthorized_WhenNonOwnerRequests() {
        when(currentUserProvider.getUserId()).thenReturn(otherUserId);
        when(itineraryRepository.findByTripIdAndActiveTrue(tripId)).thenReturn(Optional.of(testItinerary));

        assertThrows(UnauthorizedException.class, () -> itineraryService.getActiveForTrip(tripId));
    }

    @Test
    void getBudgetForTrip_ShouldSucceed_WhenOwnerRequests() {
        when(currentUserProvider.getUserId()).thenReturn(ownerId);
        when(itineraryRepository.findByTripIdAndActiveTrue(tripId)).thenReturn(Optional.of(testItinerary));

        BudgetResponse response = itineraryService.getBudgetForTrip(tripId);

        assertNotNull(response);
        assertNotNull(response.getBudgetBreakdown());
        assertEquals(BigDecimal.valueOf(30000), response.getBudgetBreakdown().getTotal());
    }

    @Test
    void getBudgetForTrip_ShouldThrowUnauthorized_WhenNonOwnerRequests() {
        when(currentUserProvider.getUserId()).thenReturn(otherUserId);
        when(itineraryRepository.findByTripIdAndActiveTrue(tripId)).thenReturn(Optional.of(testItinerary));

        assertThrows(UnauthorizedException.class, () -> itineraryService.getBudgetForTrip(tripId));
    }

    @Test
    void saveNotes_ShouldSucceed_WhenOwnerRequests() {
        when(currentUserProvider.getUserId()).thenReturn(ownerId);
        when(itineraryRepository.findByTripIdAndActiveTrue(tripId)).thenReturn(Optional.of(testItinerary));

        Map<String, Object> result = itineraryService.saveNotes(tripId, "Pack sunscreen!");

        assertTrue((Boolean) result.get("saved"));
        assertEquals("Pack sunscreen!", result.get("notes"));
        assertEquals("Pack sunscreen!", testItinerary.getPlanData().getNotes());
        verify(itineraryRepository).save(testItinerary);
    }

    @Test
    void saveNotes_ShouldThrowNotFound_WhenNoItineraryFound() {
        when(currentUserProvider.getUserId()).thenReturn(ownerId);
        when(itineraryRepository.findByTripIdAndActiveTrue(tripId)).thenReturn(Optional.empty());
        when(itineraryRepository.findTopByTripIdOrderByVersionDesc(tripId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> itineraryService.saveNotes(tripId, "Some note"));
        verify(itineraryRepository, never()).save(any());
    }

    @Test
    void saveNotes_ShouldThrowUnauthorized_WhenNonOwnerRequests() {
        when(currentUserProvider.getUserId()).thenReturn(otherUserId);
        when(itineraryRepository.findByTripIdAndActiveTrue(tripId)).thenReturn(Optional.of(testItinerary));

        assertThrows(UnauthorizedException.class, () -> itineraryService.saveNotes(tripId, "Malicious note"));
        verify(itineraryRepository, never()).save(any());
    }

    @Test
    void getHistoryForTrip_ShouldReturnOnlyCurrentUserItineraries() {
        when(currentUserProvider.getUserId()).thenReturn(ownerId);

        when(itineraryRepository.findByTripIdOrderByVersionDesc(tripId))
                .thenReturn(Optional.of(testItinerary));

        List<ItinerarySummaryResponse> history = itineraryService.getHistoryForTrip(tripId);

        assertEquals(1, history.size());
        assertEquals(itineraryId, history.get(0).getItineraryId());
    }
}

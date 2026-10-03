package com.planmytrip.trip_service.service.impl;

import com.planmytrip.trip_service.dto.request.UpdateTripPreferencesRequest;
import com.planmytrip.trip_service.dto.response.TripPreferencesResponse;
import com.planmytrip.trip_service.entity.Trip;
import com.planmytrip.trip_service.entity.TripPreference;
import com.planmytrip.trip_service.enums.TripPreferenceType;
import com.planmytrip.trip_service.enums.TripStatus;
import com.planmytrip.trip_service.enums.TripType;
import com.planmytrip.trip_service.exception.InvalidTripRequestException;
import com.planmytrip.trip_service.exception.TripNotFoundException;
import com.planmytrip.trip_service.exception.UnauthorizedException;
import com.planmytrip.trip_service.repository.TripPreferenceRepository;
import com.planmytrip.trip_service.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripPreferenceServiceImplTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripPreferenceRepository tripPreferenceRepository;

    @InjectMocks
    private TripPreferenceServiceImpl tripPreferenceService;

    private UUID tripId;
    private Long ownerId = 1L;
    private Long nonOwnerId = 2L;
    private Trip draftTrip;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        draftTrip = Trip.builder()
                .id(tripId)
                .userId(ownerId)
                .tripName("Kerala Escape")
                .destination("Kerala")
                .tripType(TripType.FAMILY)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(15))
                .status(TripStatus.DRAFT)
                .budget(BigDecimal.valueOf(40000))
                .build();
    }

    @Test
    void setPreferences_ShouldSucceed_WhenOwnerRequests() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(draftTrip));

        UpdateTripPreferencesRequest request = UpdateTripPreferencesRequest.builder()
                .preferences(List.of(TripPreferenceType.BEACHES, TripPreferenceType.WILDLIFE, TripPreferenceType.BEACHES))
                .build();

        TripPreferencesResponse response = tripPreferenceService.setPreferences(tripId, request, ownerId);

        assertNotNull(response);
        assertEquals(tripId, response.getTripId());
        // Duplicate BEACHES should be stripped, preserving unique items: [BEACHES, WILDLIFE]
        assertEquals(2, response.getPreferences().size());
        assertTrue(response.getPreferences().contains(TripPreferenceType.BEACHES));
        assertTrue(response.getPreferences().contains(TripPreferenceType.WILDLIFE));

        verify(tripPreferenceRepository).deleteAllByTripId(tripId);
        verify(tripPreferenceRepository, times(2)).save(any(TripPreference.class));
    }

    @Test
    void setPreferences_ShouldThrowUnauthorized_WhenNonOwnerRequests() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(draftTrip));

        UpdateTripPreferencesRequest request = UpdateTripPreferencesRequest.builder()
                .preferences(List.of(TripPreferenceType.TREKKING))
                .build();

        assertThrows(UnauthorizedException.class, () ->
                tripPreferenceService.setPreferences(tripId, request, nonOwnerId));

        verify(tripPreferenceRepository, never()).deleteAllByTripId(any());
        verify(tripPreferenceRepository, never()).save(any());
    }

    @Test
    void setPreferences_ShouldThrowUnauthorized_WhenUserIdIsNull() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(draftTrip));

        UpdateTripPreferencesRequest request = UpdateTripPreferencesRequest.builder()
                .preferences(List.of(TripPreferenceType.TREKKING))
                .build();

        assertThrows(UnauthorizedException.class, () ->
                tripPreferenceService.setPreferences(tripId, request, null));
    }

    @Test
    void setPreferences_ShouldThrow_WhenTripIsBooked() {
        draftTrip.setStatus(TripStatus.BOOKED);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(draftTrip));

        UpdateTripPreferencesRequest request = UpdateTripPreferencesRequest.builder()
                .preferences(List.of(TripPreferenceType.WILDLIFE))
                .build();

        assertThrows(InvalidTripRequestException.class, () ->
                tripPreferenceService.setPreferences(tripId, request, ownerId));
    }

    @Test
    void getPreferences_ShouldSucceed_WhenOwnerRequests() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(draftTrip));
        when(tripPreferenceRepository.findByTripId(tripId)).thenReturn(List.of(
                TripPreference.builder().tripId(tripId).preference(TripPreferenceType.MUSEUMS).build()
        ));

        TripPreferencesResponse response = tripPreferenceService.getPreferences(tripId, ownerId);

        assertNotNull(response);
        assertEquals(1, response.getPreferences().size());
        assertEquals(TripPreferenceType.MUSEUMS, response.getPreferences().get(0));
    }

    @Test
    void getPreferences_ShouldThrowUnauthorized_WhenNonOwnerRequests() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(draftTrip));

        assertThrows(UnauthorizedException.class, () ->
                tripPreferenceService.getPreferences(tripId, nonOwnerId));
    }

    @Test
    void getPreferences_ShouldThrowNotFound_WhenTripDoesNotExist() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

        assertThrows(TripNotFoundException.class, () ->
                tripPreferenceService.getPreferences(tripId, ownerId));
    }
}

package com.planmytrip.trip_service.service.impl;

import com.planmytrip.trip_service.dto.request.CreateTripRequest;
import com.planmytrip.trip_service.dto.request.UpdateTripRequest;
import com.planmytrip.trip_service.dto.response.PageResponse;
import com.planmytrip.trip_service.dto.response.TripResponse;
import com.planmytrip.trip_service.entity.Trip;
import com.planmytrip.trip_service.enums.TripStatus;
import com.planmytrip.trip_service.enums.TripType;
import com.planmytrip.trip_service.exception.InvalidTripRequestException;
import com.planmytrip.trip_service.exception.TripNotFoundException;
import com.planmytrip.trip_service.exception.UnauthorizedException;
import com.planmytrip.trip_service.mapper.TripMapper;
import com.planmytrip.trip_service.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceImplTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private com.planmytrip.trip_service.repository.TripPreferenceRepository tripPreferenceRepository;

    @Spy
    private TripMapper tripMapper = new TripMapper();

    @InjectMocks
    private TripServiceImpl tripService;

    private Trip testTrip;
    private UUID tripId;
    private Long ownerId = 1L;
    private Long otherUserId = 2L;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        testTrip = Trip.builder()
                .id(tripId)
                .userId(ownerId)
                .tripName("Goa Holiday")
                .destination("Goa")
                .tripType(TripType.FRIENDS)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(10))
                .adults(2)
                .children(0)
                .budget(BigDecimal.valueOf(25000))
                .currency("INR")
                .status(TripStatus.DRAFT)
                .build();
    }

    @Test
    void createTrip_ShouldSucceed_WhenValidRequest() {
        CreateTripRequest request = CreateTripRequest.builder()
                .userId(ownerId)
                .tripName("Goa Holiday")
                .destination("Goa")
                .tripType(TripType.FRIENDS)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(10))
                .adults(2)
                .children(0)
                .budget(BigDecimal.valueOf(25000))
                .currency("INR")
                .build();

        when(tripRepository.save(any(Trip.class))).thenReturn(testTrip);

        TripResponse response = tripService.createTrip(request);

        assertNotNull(response);
        assertEquals("Goa Holiday", response.getTripName());
        assertEquals(ownerId, response.getUserId());
        assertEquals(TripStatus.DRAFT, response.getStatus());
        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    void createTrip_ShouldThrow_WhenEndDateBeforeStartDate() {
        CreateTripRequest request = CreateTripRequest.builder()
                .userId(ownerId)
                .tripName("Invalid Trip")
                .destination("Goa")
                .tripType(TripType.SOLO)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(5))
                .build();

        assertThrows(InvalidTripRequestException.class, () -> tripService.createTrip(request));
        verify(tripRepository, never()).save(any());
    }

    @Test
    void getTripById_ShouldSucceed_WhenOwnerRequests() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

        TripResponse response = tripService.getTripById(tripId, ownerId);

        assertNotNull(response);
        assertEquals(tripId, response.getId());
        assertEquals("Goa", response.getDestination());
    }

    @Test
    void getTripById_ShouldThrowUnauthorized_WhenNonOwnerRequests() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

        assertThrows(UnauthorizedException.class, () -> tripService.getTripById(tripId, otherUserId));
    }

    @Test
    void getTripById_ShouldThrowNotFound_WhenTripDoesNotExist() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

        assertThrows(TripNotFoundException.class, () -> tripService.getTripById(tripId, ownerId));
    }

    @Test
    void updateTrip_ShouldSucceed_WhenOwnerUpdatesDraftTrip() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(tripRepository.save(any(Trip.class))).thenReturn(testTrip);

        UpdateTripRequest updateRequest = UpdateTripRequest.builder()
                .tripName("Goa Beach Trip Updated")
                .budget(BigDecimal.valueOf(35000))
                .build();

        TripResponse response = tripService.updateTrip(tripId, updateRequest, ownerId);

        assertNotNull(response);
        verify(tripRepository).save(testTrip);
        assertEquals("Goa Beach Trip Updated", testTrip.getTripName());
    }

    @Test
    void updateTrip_ShouldThrowUnauthorized_WhenNonOwnerUpdates() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

        UpdateTripRequest updateRequest = UpdateTripRequest.builder().tripName("Hacked").build();

        assertThrows(UnauthorizedException.class, () -> tripService.updateTrip(tripId, updateRequest, otherUserId));
        verify(tripRepository, never()).save(any());
    }

    @Test
    void updateTrip_ShouldThrow_WhenTripIsBooked() {
        testTrip.setStatus(TripStatus.BOOKED);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

        UpdateTripRequest updateRequest = UpdateTripRequest.builder().tripName("New Name").build();

        assertThrows(InvalidTripRequestException.class, () -> tripService.updateTrip(tripId, updateRequest, ownerId));
    }

    @Test
    void deleteTrip_ShouldSucceed_WhenOwnerDeletesDraftTrip() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

        tripService.deleteTrip(tripId, ownerId);

        verify(tripPreferenceRepository).deleteAllByTripId(tripId);
        verify(tripRepository).delete(testTrip);
    }

    @Test
    void deleteTrip_ShouldThrowUnauthorized_WhenNonOwnerDeletes() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

        assertThrows(UnauthorizedException.class, () -> tripService.deleteTrip(tripId, otherUserId));
        verify(tripRepository, never()).delete(any());
        verify(tripPreferenceRepository, never()).deleteAllByTripId(any());
    }

    @Test
    void getTripById_ShouldThrowUnauthorized_WhenUserIdIsNull() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

        assertThrows(UnauthorizedException.class, () -> tripService.getTripById(tripId, null));
    }

    @Test
    void updateTrip_ShouldThrowUnauthorized_WhenUserIdIsNull() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

        UpdateTripRequest updateRequest = UpdateTripRequest.builder().tripName("Updated").build();
        assertThrows(UnauthorizedException.class, () -> tripService.updateTrip(tripId, updateRequest, null));
        verify(tripRepository, never()).save(any());
    }

    @Test
    void deleteTrip_ShouldThrowUnauthorized_WhenUserIdIsNull() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

        assertThrows(UnauthorizedException.class, () -> tripService.deleteTrip(tripId, null));
        verify(tripRepository, never()).delete(any());
    }

    @Test
    void updateTripStatus_ShouldFollowCorrectProgression() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(tripRepository.save(any(Trip.class))).thenReturn(testTrip);

        // DRAFT -> PLAN_READY (allowed)
        TripResponse response = tripService.updateTripStatus(tripId, TripStatus.PLAN_READY, ownerId);
        assertNotNull(response);
        assertEquals(TripStatus.PLAN_READY, testTrip.getStatus());
    }

    @Test
    void updateTripStatus_ShouldThrow_WhenSkippingProgression() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

        // DRAFT -> COMPLETED (invalid transition)
        assertThrows(InvalidTripRequestException.class, () ->
                tripService.updateTripStatus(tripId, TripStatus.COMPLETED, ownerId));
    }

    @Test
    void listTrips_ShouldReturnPagedResults() {
        when(tripRepository.findByUserId(eq(ownerId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testTrip)));

        PageResponse<TripResponse> pageResponse = tripService.listTrips(ownerId, null, 0, 10);

        assertNotNull(pageResponse);
        assertEquals(1, pageResponse.getContent().size());
        assertEquals("Goa Holiday", pageResponse.getContent().get(0).getTripName());
    }
}

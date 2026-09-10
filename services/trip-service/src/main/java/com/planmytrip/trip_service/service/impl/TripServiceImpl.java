package com.planmytrip.trip_service.service.impl;

import com.planmytrip.trip_service.dto.request.CreateTripRequest;
import com.planmytrip.trip_service.dto.request.UpdateTripRequest;
import com.planmytrip.trip_service.dto.response.PageResponse;
import com.planmytrip.trip_service.dto.response.TripResponse;
import com.planmytrip.trip_service.entity.Trip;
import com.planmytrip.trip_service.enums.TripStatus;
import com.planmytrip.trip_service.exception.InvalidTripRequestException;
import com.planmytrip.trip_service.exception.TripNotFoundException;
import com.planmytrip.trip_service.mapper.TripMapper;
import com.planmytrip.trip_service.repository.TripRepository;
import com.planmytrip.trip_service.service.TripService;
import org.springframework.data.domain.Page;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private static final Logger log = LoggerFactory.getLogger(TripServiceImpl.class);
    
    private static final int MAX_PAGE_SIZE = 50;

    private final TripRepository tripRepository;
    private final TripMapper tripMapper;

    @Override
    @Transactional
    public TripResponse createTrip(CreateTripRequest request) {
        validateDates(request);

        Trip trip = tripMapper.toEntity(request);
        Trip saved = tripRepository.save(trip);

        log.info("Created trip id={} for userId={} destination={}",
                saved.getId(), saved.getUserId(), saved.getDestination());

        return tripMapper.toResponse(saved);
    }

    @Override
    public PageResponse<TripResponse> listTrips(UUID userId, TripStatus status, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);

        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Trip> tripPage = (status != null)
                ? tripRepository.findByUserIdAndStatus(userId, status, pageRequest)
                : tripRepository.findByUserId(userId, pageRequest);

        return PageResponse.from(tripPage.map(tripMapper::toResponse));
    }

    @Override
    public TripResponse getTripById(UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        return tripMapper.toResponse(trip);
    }

    @Override
    @Transactional
    public TripResponse updateTrip(UUID tripId, UpdateTripRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        assertTripIsEditable(trip);

        tripMapper.applyUpdate(trip, request);
        validateDates(trip);

        Trip saved = tripRepository.save(trip);

        log.info("Updated trip id={}", saved.getId());

        return tripMapper.toResponse(saved);
    }
    
    @Override
    @Transactional
    public void deleteTrip(UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(()-> new TripNotFoundException(tripId));
        assertTripIsMutable(trip, "delete");
        
        tripRepository.delete(trip);
        log.info("deleted trip id={}", tripId);
    }

    @Override
    @Transactional
    public TripResponse updateTripStatus(UUID tripId, TripStatus newStatus) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        TripStatus current = trip.getStatus();
        if (!current.canTransitionTo(newStatus)) {
            throw new InvalidTripRequestException(
                    "Cannot move trip from " + current + " to " + newStatus
                            + " — status must progress one step at a time"
                            + (current.next() != null ? " (next allowed: " + current.next() + ")" : " (this status is terminal)"));
        }

        trip.setStatus(newStatus);
        Trip saved = tripRepository.save(trip);

        log.info("Trip id={} status changed {} -> {}", saved.getId(), current, newStatus);

        return tripMapper.toResponse(saved);
    }

    private void assertTripIsMutable(Trip trip, String action) {
        if (trip.getStatus() == TripStatus.BOOKED || trip.getStatus() == TripStatus.COMPLETED) {
            throw new InvalidTripRequestException(
                    "Cannot " + action + " a trip that is " + trip.getStatus());
        }
    }

    private void assertTripIsEditable(Trip trip) {
        if (trip.getStatus() == TripStatus.BOOKED || trip.getStatus() == TripStatus.COMPLETED) {
            throw new InvalidTripRequestException(
                    "Cannot edit a trip that is " + trip.getStatus());
        }
    }

    private void validateDates(CreateTripRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidTripRequestException("endDate cannot be before startDate");
        }
    }

    private void validateDates(Trip trip) {
        if (trip.getEndDate().isBefore(trip.getStartDate())) {
            throw new InvalidTripRequestException("endDate cannot be before startDate");
        }
    }
}

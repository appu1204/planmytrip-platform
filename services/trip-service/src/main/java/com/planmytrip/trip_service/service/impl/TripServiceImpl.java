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
    private final com.planmytrip.trip_service.repository.TripPreferenceRepository tripPreferenceRepository;

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
    public PageResponse<TripResponse> listTrips(Long userId, TripStatus status, int page, int size) {
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
        return getTripById(tripId, null);
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(
            value = com.planmytrip.trip_service.config.RedisConfig.CACHE_TRIPS,
            key = "#tripId.toString() + ':' + (#userId != null ? #userId : '0')"
    )
    public TripResponse getTripById(UUID tripId, Long userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        assertUserOwnsTrip(trip, userId);
        return tripMapper.toResponse(trip);
    }

    @Override
    @Transactional
    public TripResponse updateTrip(UUID tripId, UpdateTripRequest request) {
        return updateTrip(tripId, request, null);
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(
            value = com.planmytrip.trip_service.config.RedisConfig.CACHE_TRIPS,
            key = "#tripId.toString() + ':' + (#userId != null ? #userId : '0')"
    )
    public TripResponse updateTrip(UUID tripId, UpdateTripRequest request, Long userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        assertUserOwnsTrip(trip, userId);
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
        deleteTrip(tripId, null);
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(
            value = com.planmytrip.trip_service.config.RedisConfig.CACHE_TRIPS,
            key = "#tripId.toString() + ':' + (#userId != null ? #userId : '0')"
    )
    public void deleteTrip(UUID tripId, Long userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        assertUserOwnsTrip(trip, userId);
        assertTripIsMutable(trip, "delete");
        
        tripPreferenceRepository.deleteAllByTripId(tripId);
        tripRepository.delete(trip);
        log.info("deleted trip id={}", tripId);
    }

    @Override
    @Transactional
    public TripResponse updateTripStatus(UUID tripId, TripStatus newStatus) {
        return updateTripStatus(tripId, newStatus, null);
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(
            value = com.planmytrip.trip_service.config.RedisConfig.CACHE_TRIPS,
            key = "#tripId.toString() + ':' + (#userId != null ? #userId : '0')"
    )
    public TripResponse updateTripStatus(UUID tripId, TripStatus newStatus, Long userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        assertUserOwnsTrip(trip, userId);

        TripStatus current = trip.getStatus();
        if (!current.canTransitionTo(newStatus)) {
            String allowedMsg = (current.next() != null)
                    ? " (next allowed: " + current.next() + ", or CANCELLED)"
                    : " (this status is terminal)";
            throw new InvalidTripRequestException(
                    "Cannot move trip from " + current + " to " + newStatus
                            + " — status must progress one step at a time or be CANCELLED" + allowedMsg);
        }

        trip.setStatus(newStatus);
        Trip saved = tripRepository.save(trip);

        log.info("Trip id={} status changed {} -> {}", saved.getId(), current, newStatus);

        return tripMapper.toResponse(saved);
    }

    private void assertUserOwnsTrip(Trip trip, Long userId) {
        if (userId == null) {
            throw new com.planmytrip.trip_service.exception.UnauthorizedException(
                    "User authentication is required");
        }
        if (!userId.equals(trip.getUserId())) {
            throw new com.planmytrip.trip_service.exception.UnauthorizedException(
                    "You do not have permission to access or modify this trip");
        }
    }

    private void assertTripIsMutable(Trip trip, String action) {
        if (trip.getStatus() == TripStatus.BOOKED || trip.getStatus() == TripStatus.COMPLETED) {
            throw new InvalidTripRequestException(
                    "Cannot " + action + " a trip that is " + trip.getStatus());
        }
    }

    private void assertTripIsEditable(Trip trip) {
        if (trip.getStatus() == TripStatus.BOOKED || trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
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

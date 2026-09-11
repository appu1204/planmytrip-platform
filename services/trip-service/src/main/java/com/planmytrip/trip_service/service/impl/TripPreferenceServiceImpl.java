package com.planmytrip.trip_service.service.impl;

import com.planmytrip.trip_service.dto.request.UpdateTripPreferencesRequest;
import com.planmytrip.trip_service.dto.response.TripPreferencesResponse;
import com.planmytrip.trip_service.entity.Trip;
import com.planmytrip.trip_service.entity.TripPreference;
import com.planmytrip.trip_service.enums.TripPreferenceType;
import com.planmytrip.trip_service.enums.TripStatus;
import com.planmytrip.trip_service.exception.InvalidTripRequestException;
import com.planmytrip.trip_service.exception.TripNotFoundException;
import com.planmytrip.trip_service.repository.TripPreferenceRepository;
import com.planmytrip.trip_service.repository.TripRepository;
import com.planmytrip.trip_service.service.TripPreferenceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TripPreferenceServiceImpl implements TripPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(TripPreferenceServiceImpl.class);

    private final TripRepository tripRepository;
    private final TripPreferenceRepository tripPreferenceRepository;

    @Override
    @Transactional
    public TripPreferencesResponse setPreferences(UUID tripId, UpdateTripPreferencesRequest request) {
        Trip trip = getTripOrThrow(tripId);
        assertTripIsEditable(trip);

        // De-duplicate while preserving the order the user picked them in.
        Set<TripPreferenceType> distinctPreferences = new LinkedHashSet<>(request.getPreferences());

        tripPreferenceRepository.deleteAllByTripId(tripId);

        distinctPreferences.forEach(pref ->
                tripPreferenceRepository.save(TripPreference.builder()
                        .tripId(tripId)
                        .preference(pref)
                        .build()));

        log.info("Set {} preference(s) for tripId={}", distinctPreferences.size(), tripId);

        return TripPreferencesResponse.builder()
                .tripId(tripId)
                .preferences(List.copyOf(distinctPreferences))
                .build();
    }

    @Override
    public TripPreferencesResponse getPreferences(UUID tripId) {
        getTripOrThrow(tripId); // 404 cleanly if the trip doesn't exist

        List<TripPreferenceType> preferences = tripPreferenceRepository.findByTripId(tripId)
                .stream()
                .map(TripPreference::getPreference)
                .toList();

        return TripPreferencesResponse.builder()
                .tripId(tripId)
                .preferences(preferences)
                .build();
    }

    private Trip getTripOrThrow(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
    }

    private void assertTripIsEditable(Trip trip) {
        if (trip.getStatus() == TripStatus.BOOKED || trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new InvalidTripRequestException(
                    "Cannot change preferences for a trip that is " + trip.getStatus());
        }
    }
}

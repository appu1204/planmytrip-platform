package com.planmytrip.trip_service.service.impl;

import com.planmytrip.trip_service.dto.request.CreateTripRequest;
import com.planmytrip.trip_service.dto.response.TripResponse;
import com.planmytrip.trip_service.entity.Trip;
import com.planmytrip.trip_service.exception.InvalidTripRequestException;
import com.planmytrip.trip_service.mapper.TripMapper;
import com.planmytrip.trip_service.repository.TripRepository;
import com.planmytrip.trip_service.service.TripService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private static final Logger log = LoggerFactory.getLogger(TripServiceImpl.class);

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

    private void validateDates(CreateTripRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidTripRequestException("endDate cannot be before startDate");
        }
    }
}

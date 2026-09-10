package com.planmytrip.trip_service.service.impl;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.planmytrip.trip_service.dto.response.DestinationResponse;
import com.planmytrip.trip_service.entity.Destination;
import com.planmytrip.trip_service.enums.TripType;
import com.planmytrip.trip_service.repository.DestinationRepository;
import com.planmytrip.trip_service.service.DestinationService;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor

public class DestinationServiceImpl implements DestinationService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final DestinationRepository destinationRepository;

    @Override
    public List<DestinationResponse> getPopularDestinations(TripType persona, Integer limit) {
        int safeLimit = normaliseLimit(limit);
        PageRequest page = PageRequest.of(0, safeLimit);

        List<Destination> destinations = (persona != null)
                ? destinationRepository.findByPersonaAndActiveTrueOrderByPopularityRankAsc(persona, page)
                : destinationRepository.findByActiveTrueOrderByPopularityRankAsc(page);

        return destinations.stream()
                    .map(this::toResponse)
                    .toList();

    }

    private int normaliseLimit(Integer requested) {
        if (requested == null || requested <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(requested, MAX_LIMIT);
    }

    private DestinationResponse toResponse(Destination d) {
        
        return DestinationResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .country(d.getCountry())
                .imageUrl(d.getImageUrl())
                .persona(d.getPersona())
                .popularityRank(d.getPopularityRank())
                .build();
    }
}

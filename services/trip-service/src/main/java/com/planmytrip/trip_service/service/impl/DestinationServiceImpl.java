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

    private static final int DEFAULT_LIMIT = 6;
    private static final int MAX_LIMIT = 20;

    private final DestinationRepository destinationRepository;

    @Override
    @org.springframework.cache.annotation.Cacheable(
            value = com.planmytrip.trip_service.config.RedisConfig.CACHE_POPULAR_DESTINATIONS,
            key = "(#persona != null ? #persona.name() : 'ALL') + ':' + (#limit != null && #limit > 0 ? (#limit > 20 ? 20 : #limit) : 6)"
    )
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
        double rating = 4.80 + ((d.getPopularityRank() != null ? d.getPopularityRank() : 1) % 4) * 0.05;
        if (rating > 4.98) rating = 4.95;

        String tag = "Trending Destination";
        String price = "₹22,000";

        if (d.getPersona() != null) {
            switch (d.getPersona()) {
                case FAMILY:
                    tag = "Family Favorite";
                    price = "₹28,500";
                    break;
                case COUPLE:
                    tag = "Romantic Escape";
                    price = "₹42,000";
                    break;
                case FRIENDS:
                    tag = "Group Getaway";
                    price = "₹19,000";
                    break;
                case ADVENTURE:
                    tag = "Adventure & Treks";
                    price = "₹16,500";
                    break;
                case SOLO:
                    tag = "Solo Expedition";
                    price = "₹21,000";
                    break;
            }
        }

        return DestinationResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .country(d.getCountry())
                .imageUrl(d.getImageUrl())
                .image(d.getImageUrl())
                .persona(d.getPersona())
                .popularityRank(d.getPopularityRank())
                .rating(Math.round(rating * 100.0) / 100.0)
                .tag(tag)
                .priceFrom(price)
                .build();
    }
}

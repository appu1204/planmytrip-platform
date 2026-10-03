package com.planmytrip.trip_service.service.impl;

import com.planmytrip.trip_service.dto.response.DestinationResponse;
import com.planmytrip.trip_service.entity.Destination;
import com.planmytrip.trip_service.enums.TripType;
import com.planmytrip.trip_service.repository.DestinationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DestinationServiceImplTest {

    @Mock
    private DestinationRepository destinationRepository;

    @InjectMocks
    private DestinationServiceImpl destinationService;

    @Test
    void getPopularDestinations_ShouldReturnMappedResponses_WhenPersonaIsNull() {
        Destination d1 = Destination.builder()
                .id(UUID.randomUUID())
                .name("Paris")
                .country("France")
                .active(true)
                .popularityRank(1)
                .persona(TripType.SOLO)
                .build();

        when(destinationRepository.findByActiveTrueOrderByPopularityRankAsc(any(Pageable.class)))
                .thenReturn(List.of(d1));

        List<DestinationResponse> results = destinationService.getPopularDestinations(null, 5);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Paris", results.get(0).getName());
        assertEquals("France", results.get(0).getCountry());
        verify(destinationRepository).findByActiveTrueOrderByPopularityRankAsc(any(Pageable.class));
    }

    @Test
    void getPopularDestinations_ShouldFilterByPersona_WhenPersonaProvided() {
        Destination d1 = Destination.builder()
                .id(UUID.randomUUID())
                .name("Goa")
                .country("India")
                .active(true)
                .popularityRank(2)
                .persona(TripType.FAMILY)
                .build();

        when(destinationRepository.findByPersonaAndActiveTrueOrderByPopularityRankAsc(eq(TripType.FAMILY), any(Pageable.class)))
                .thenReturn(List.of(d1));

        List<DestinationResponse> results = destinationService.getPopularDestinations(TripType.FAMILY, 10);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Goa", results.get(0).getName());
        verify(destinationRepository).findByPersonaAndActiveTrueOrderByPopularityRankAsc(eq(TripType.FAMILY), any(Pageable.class));
    }
}

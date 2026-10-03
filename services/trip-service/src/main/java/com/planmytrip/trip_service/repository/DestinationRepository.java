package com.planmytrip.trip_service.repository;

import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.planmytrip.trip_service.entity.Destination;
import com.planmytrip.trip_service.enums.TripType;



public interface DestinationRepository extends JpaRepository <Destination, UUID> {
    List<Destination> findByPersonaAndActiveTrueOrderByPopularityRankAsc(TripType persona, Pageable pageable);
    List<Destination> findByActiveTrueOrderByPopularityRankAsc(Pageable pageable);

}
